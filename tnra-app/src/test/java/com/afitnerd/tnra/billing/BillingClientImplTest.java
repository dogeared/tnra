package com.afitnerd.tnra.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class BillingClientImplTest {

    private MockRestServiceServer server;
    private BillingClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BillingClientImpl(builder, "https://billing.local", "group-token");
    }

    @Test
    void isEntitled_true() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andExpect(method(GET))
            .andExpect(queryParam("email", "m@x.com"))
            .andExpect(header("Authorization", "Bearer group-token"))
            .andRespond(withSuccess("{\"entitled\":true,\"status\":\"ACTIVE\"}", APPLICATION_JSON));

        assertTrue(client.isEntitled("m@x.com"));
        server.verify();
    }

    @Test
    void isEntitled_false() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":false,\"status\":\"SUSPENDED\"}", APPLICATION_JSON));

        assertFalse(client.isEntitled("m@x.com"));
    }

    @Test
    void isEntitled_cachesWithinTtl() {
        // Only ONE response is queued; a second network call would fail as "unexpected request".
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":true}", APPLICATION_JSON));

        assertTrue(client.isEntitled("m@x.com"));
        assertTrue(client.isEntitled("m@x.com")); // served from cache
        server.verify();
    }

    @Test
    void isEntitledFresh_bypassesCache_hitsServiceEachTime() {
        // Two responses queued; the cache must NOT short-circuit the second call.
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":false}", APPLICATION_JSON));
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":true}", APPLICATION_JSON));

        assertFalse(client.isEntitledFresh("m@x.com"));
        assertTrue(client.isEntitledFresh("m@x.com")); // not served from cache — sees the new value
        server.verify();
    }

    @Test
    void isEntitledFresh_refreshesCache_soSubsequentIsEntitledIsServedFromIt() {
        // Only ONE response queued; if the follow-up isEntitled hit the network, verify would fail.
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":true}", APPLICATION_JSON));

        assertTrue(client.isEntitledFresh("m@x.com"));
        assertTrue(client.isEntitled("m@x.com")); // served from the cache the fresh call populated
        server.verify();
    }

    @Test
    void entitlement_returnsEntitledAndRawStatus() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andExpect(queryParam("email", "m@x.com"))
            .andRespond(withSuccess("{\"entitled\":true,\"status\":\"ON_GRACE_PERIOD\"}", APPLICATION_JSON));

        BillingClient.Entitlement e = client.entitlement("m@x.com");

        assertTrue(e.entitled());
        assertEquals("ON_GRACE_PERIOD", e.status());
        server.verify();
    }

    @Test
    void entitlement_parsesPayerEmail_forAGiftedMembership() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess(
                "{\"entitled\":true,\"status\":\"ACTIVE\",\"payerEmail\":\"alice@x.com\"}", APPLICATION_JSON));

        BillingClient.Entitlement e = client.entitlement("m@x.com");

        assertEquals("alice@x.com", e.payerEmail());
    }

    @Test
    void entitlement_payerEmailNull_forSelfPay() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withSuccess("{\"entitled\":true,\"status\":\"ACTIVE\"}", APPLICATION_JSON));

        assertNull(client.entitlement("m@x.com").payerEmail());
    }

    @Test
    void entitlement_reportsNotSettledOnError() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withServerError());

        BillingClient.Entitlement e = client.entitlement("m@x.com");

        assertFalse(e.entitled()); // can't determine → not settled, gift may proceed (server enforces)
        assertEquals("", e.status());
    }

    @Test
    void isEntitled_failsOpenOnError() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/entitlement")))
            .andRespond(withServerError());

        assertTrue(client.isEntitled("m@x.com")); // central down → fail open
    }

    @Test
    void createCheckout_sendsBodyAndReturnsUrl() {
        server.expect(requestTo("https://billing.local/api/v1/checkout"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.beneficiaryEmail").value("ben@x.com"))
            .andExpect(jsonPath("$.variant").value("monthly"))
            .andExpect(jsonPath("$.payerEmail").value("admin@x.com"))
            .andExpect(jsonPath("$.redirectUrl").value("https://app/back"))
            .andRespond(withSuccess("{\"url\":\"https://pay/1\"}", APPLICATION_JSON));

        assertEquals("https://pay/1",
            client.createCheckout("ben@x.com", "monthly", "admin@x.com", "https://app/back"));
        server.verify();
    }

    @Test
    void portalUrl_returnsUrl() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/portal")))
            .andExpect(queryParam("email", "m@x.com"))
            .andRespond(withSuccess("{\"url\":\"https://portal/1\"}", APPLICATION_JSON));

        assertEquals("https://portal/1", client.portalUrl("m@x.com"));
    }

    @Test
    void covering_returnsList() {
        server.expect(requestTo(startsWith("https://billing.local/api/v1/covering")))
            .andExpect(queryParam("payerEmail", "admin@x.com"))
            .andRespond(withSuccess(
                "[{\"email\":\"a@x.com\",\"status\":\"ACTIVE\"},{\"email\":\"b@x.com\",\"status\":\"PENDING_PAYMENT\"}]",
                APPLICATION_JSON));

        List<BillingClient.CoveredMember> covering = client.covering("admin@x.com");

        assertEquals(2, covering.size());
        assertEquals("a@x.com", covering.get(0).email());
        assertEquals("ACTIVE", covering.get(0).status());
    }
}
