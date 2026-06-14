package com.afitnerd.tnra.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            .andRespond(withSuccess("{\"url\":\"https://pay/1\"}", APPLICATION_JSON));

        assertEquals("https://pay/1", client.createCheckout("ben@x.com", "monthly", "admin@x.com"));
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
