package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.LemonSqueezyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

class LemonSqueezyClientImplTest {

    private static final MediaType JSON_API = MediaType.parseMediaType("application/vnd.api+json");

    private MockRestServiceServer server;
    private LemonSqueezyClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        LemonSqueezyProperties props = new LemonSqueezyProperties();
        props.setApiKey("test-key");
        props.setStoreId("store-1");
        props.getVariant().setMonthly("var-monthly");
        props.getVariant().setYearly("var-yearly");
        client = new LemonSqueezyClientImpl(builder, props);
    }

    @Test
    void createCheckout_buildsRequestAndReturnsUrl() {
        server.expect(requestTo("https://api.lemonsqueezy.com/v1/checkouts"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.data.relationships.variant.data.id").value("var-monthly"))
            .andExpect(jsonPath("$.data.attributes.checkout_data.email").value("payer@x.com"))
            .andExpect(jsonPath("$.data.attributes.checkout_data.custom.beneficiary_email").value("ben@x.com"))
            .andRespond(withSuccess("{\"data\":{\"attributes\":{\"url\":\"https://pay.ls/abc\"}}}", JSON_API));

        String url = client.createCheckout("rome", "ben@x.com", "payer@x.com", "monthly");

        assertEquals("https://pay.ls/abc", url);
        server.verify();
    }

    @Test
    void createCheckout_unknownVariant_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> client.createCheckout("rome", "ben@x.com", "payer@x.com", "weekly"));
    }

    @Test
    void createCheckout_missingUrlInResponse_throws() {
        server.expect(requestTo("https://api.lemonsqueezy.com/v1/checkouts"))
            .andRespond(withSuccess("{\"data\":{\"attributes\":{}}}", JSON_API));

        assertThrows(IllegalStateException.class,
            () -> client.createCheckout("rome", "ben@x.com", "payer@x.com", "yearly"));
    }

    @Test
    void getCustomerPortalUrl_returnsPortal() {
        server.expect(requestTo("https://api.lemonsqueezy.com/v1/subscriptions/sub_9"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                "{\"data\":{\"attributes\":{\"urls\":{\"customer_portal\":\"https://portal.ls/9\"}}}}", JSON_API));

        assertEquals("https://portal.ls/9", client.getCustomerPortalUrl("sub_9"));
        server.verify();
    }
}
