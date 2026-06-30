package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.PaddleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PaddleClientImplTest {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;
    private static final String BASE = "https://sandbox-api.paddle.com";

    private MockRestServiceServer server;
    private PaddleClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        PaddleProperties props = new PaddleProperties();
        props.setApiKey("test-key");
        props.setApiBase(BASE);
        props.getPrice().setMonthly("pri_monthly");
        props.getPrice().setYearly("pri_yearly");
        client = new PaddleClientImpl(builder, props);
    }

    @Test
    void createCheckout_buildsTransactionAndReturnsCheckoutUrl() {
        server.expect(requestTo(BASE + "/transactions"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.items[0].price_id").value("pri_monthly"))
            .andExpect(jsonPath("$.items[0].quantity").value(1))
            .andExpect(jsonPath("$.customer.email").value("payer@x.com"))
            .andExpect(jsonPath("$.custom_data.group_slug").value("rome"))
            .andExpect(jsonPath("$.custom_data.beneficiary_email").value("ben@x.com"))
            .andExpect(jsonPath("$.custom_data.redirect_url").value("https://app.local/back"))
            .andExpect(jsonPath("$.collection_mode").value("automatic"))
            .andRespond(withSuccess("{\"data\":{\"checkout\":{\"url\":\"https://pay.paddle/abc\"}}}", JSON));

        String url = client.createCheckout("rome", "ben@x.com", "payer@x.com", "monthly", "https://app.local/back");

        assertEquals("https://pay.paddle/abc", url);
        server.verify();
    }

    @Test
    void createCheckout_omitsRedirectWhenBlank() {
        server.expect(requestTo(BASE + "/transactions"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.custom_data.redirect_url").doesNotExist())
            .andRespond(withSuccess("{\"data\":{\"checkout\":{\"url\":\"https://pay.paddle/x\"}}}", JSON));

        assertEquals("https://pay.paddle/x",
            client.createCheckout("rome", "ben@x.com", "payer@x.com", "monthly", ""));
        server.verify();
    }

    @Test
    void createCheckout_unknownVariant_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> client.createCheckout("rome", "ben@x.com", "payer@x.com", "weekly", null));
    }

    @Test
    void createCheckout_missingUrlInResponse_throws() {
        server.expect(requestTo(BASE + "/transactions"))
            .andRespond(withSuccess("{\"data\":{}}", JSON));

        assertThrows(IllegalStateException.class,
            () -> client.createCheckout("rome", "ben@x.com", "payer@x.com", "yearly", null));
    }

    @Test
    void cancelSubscription_postsCancelImmediately() {
        server.expect(requestTo(BASE + "/subscriptions/sub_old/cancel"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andExpect(jsonPath("$.effective_from").value("immediately"))
            .andRespond(withSuccess("{\"data\":{}}", JSON));

        client.cancelSubscription("sub_old");
        server.verify();
    }

    @Test
    void getCustomerPortalUrl_postsPortalSession_returnsOverview() {
        server.expect(requestTo(BASE + "/customers/ctm_9/portal-sessions"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andRespond(withSuccess(
                "{\"data\":{\"urls\":{\"general\":{\"overview\":\"https://portal.paddle/9\"}}}}", JSON));

        assertEquals("https://portal.paddle/9", client.getCustomerPortalUrl("ctm_9"));
        server.verify();
    }
}
