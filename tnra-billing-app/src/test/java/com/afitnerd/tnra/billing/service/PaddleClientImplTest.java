package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.PaddleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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

    @Test
    void getUpdatePaymentMethodCheckoutUrl_readsCheckoutUrl() {
        server.expect(requestTo(BASE + "/subscriptions/sub_7/update-payment-method-transaction"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"ready\",\"checkout\":{\"url\":\"https://tnra.app/pay?_ptxn=txn_u\"}}}", JSON));

        assertEquals("https://tnra.app/pay?_ptxn=txn_u", client.getUpdatePaymentMethodCheckoutUrl("sub_7"));
        server.verify();
    }

    @Test
    void getUpdatePaymentMethodCheckoutUrl_failsLoudlyWithoutUrl() {
        server.expect(requestTo(BASE + "/subscriptions/sub_7/update-payment-method-transaction"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"draft\",\"checkout\":{\"url\":null}}}", JSON));

        assertThrows(IllegalStateException.class, () -> client.getUpdatePaymentMethodCheckoutUrl("sub_7"));
    }

    @Test
    void getSubscription_parsesDates_scheduledCancel_andTrial() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer test-key"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"trialing\","
                + "\"current_billing_period\":{\"starts_at\":\"2026-10-13T20:03:10.56Z\",\"ends_at\":\"2026-12-12T20:03:10.56Z\"},"
                + "\"next_billed_at\":null,"
                + "\"scheduled_change\":{\"action\":\"cancel\",\"effective_at\":\"2026-12-12T20:03:10.56Z\",\"resume_at\":null},"
                + "\"billing_cycle\":{\"frequency\":1,\"interval\":\"month\"},"
                + "\"items\":[{\"trial_dates\":{\"starts_at\":\"2026-10-13T20:03:10.56Z\",\"ends_at\":\"2026-12-12T20:03:10.56Z\"}}]}}", JSON));

        PaymentProviderClient.SubscriptionDetails d = client.getSubscription("sub_1");

        assertEquals("trialing", d.status());
        assertEquals(java.time.Instant.parse("2026-12-12T20:03:10.560Z"), d.currentPeriodEndsAt());
        assertNull(d.nextBilledAt());
        assertEquals("cancel", d.scheduledAction());
        assertEquals(java.time.Instant.parse("2026-12-12T20:03:10.560Z"), d.scheduledEffectiveAt());
        assertEquals(java.time.Instant.parse("2026-12-12T20:03:10.560Z"), d.trialEndsAt());
        assertEquals("month", d.interval());
    }

    @Test
    void getSubscription_toleratesMissingOptionalFields() {
        server.expect(requestTo(BASE + "/subscriptions/sub_2"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"active\",\"next_billed_at\":\"2026-11-13T00:00:00Z\","
                + "\"scheduled_change\":null,\"items\":[],\"billing_cycle\":{\"interval\":\"year\"}}}", JSON));

        PaymentProviderClient.SubscriptionDetails d = client.getSubscription("sub_2");

        assertEquals("active", d.status());
        assertNull(d.currentPeriodEndsAt());
        assertEquals(java.time.Instant.parse("2026-11-13T00:00:00Z"), d.nextBilledAt());
        assertNull(d.scheduledAction());
        assertNull(d.trialEndsAt());
        assertEquals("year", d.interval());
    }

    @Test
    void scheduleCancellation_atNextBillingPeriod_returnsEffectiveDate() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1/cancel"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.effective_from").value("next_billing_period"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"active\",\"scheduled_change\":{\"action\":\"cancel\",\"effective_at\":\"2026-12-12T20:03:10Z\"}}}", JSON));

        assertEquals(java.time.Instant.parse("2026-12-12T20:03:10Z"), client.scheduleCancellation("sub_1"));
        server.verify();
    }

    @Test
    void scheduleCancellation_failsLoudlyWithoutDate() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1/cancel"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"active\",\"scheduled_change\":null}}", JSON));
        assertThrows(IllegalStateException.class, () -> client.scheduleCancellation("sub_1"));
    }

    @Test
    void removeScheduledCancellation_patchesScheduledChangeToNull() {
        server.expect(requestTo(BASE + "/subscriptions/sub_1"))
            .andExpect(method(org.springframework.http.HttpMethod.PATCH))
            .andExpect(content().json("{\"scheduled_change\":null}"))
            .andRespond(withSuccess("{\"data\":{\"status\":\"active\"}}", JSON));

        client.removeScheduledCancellation("sub_1");
        server.verify();
    }
}
