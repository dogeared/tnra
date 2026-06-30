package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.PaddleProperties;
import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.BillingWebhookEvent;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.BillingWebhookEventRepository;
import com.afitnerd.tnra.billing.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {

    private static final String SECRET = "test-webhook-secret";

    private BillingWebhookEventRepository eventRepo;
    private BillingAccountRepository accountRepo;
    private PaymentProviderClient providerClient;
    private PaddleProperties props;
    private WebhookService service;

    @BeforeEach
    void setUp() {
        eventRepo = mock(BillingWebhookEventRepository.class);
        accountRepo = mock(BillingAccountRepository.class);
        providerClient = mock(PaymentProviderClient.class);
        props = new PaddleProperties();
        props.setWebhookSecret(SECRET);
        service = new WebhookService(eventRepo, accountRepo, providerClient, props, new ObjectMapper());
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Paddle subscription event with a chosen event type, subscription id, and payer (gift/self-pay). */
    private String body(String eventType, String subId, String payerEmail) {
        return "{\"event_type\":\"" + eventType + "\","
            + "\"data\":{\"id\":\"" + subId + "\",\"status\":\"active\",\"customer_id\":\"ctm_42\","
            + "\"custom_data\":{\"group_slug\":\"rome\",\"beneficiary_email\":\"m@x.com\","
            + "\"payer_email\":\"" + payerEmail + "\"}}}";
    }

    /** Build a valid Paddle-Signature header (ts=<unix>;h1=HMAC-SHA256(ts:body, secret)). */
    private String sign(String body) {
        String ts = "1700000000";
        return "ts=" + ts + ";h1=" + HashUtil.hmacSha256Hex(ts + ":" + body, SECRET);
    }

    private String createdBody() {
        return body("subscription.created", "sub_1", "m@x.com");
    }

    @Test
    void invalidSignature_unauthorized_andNothingPersisted() {
        String body = createdBody();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.process(body, "ts=1700000000;h1=deadbeef"));
        assertEquals(401, ex.getStatusCode().value());
        verify(eventRepo, never()).save(any());
    }

    @Test
    void malformedSignatureHeader_unauthorized() {
        assertThrows(ResponseStatusException.class, () -> service.process(createdBody(), "garbage"));
    }

    @Test
    void blankSecret_unauthorized() {
        props.setWebhookSecret("");
        assertThrows(ResponseStatusException.class, () -> service.process(createdBody(), "x"));
    }

    @Test
    void duplicateEvent_isNoOp() {
        String body = createdBody();
        when(eventRepo.existsByLsEventId(HashUtil.sha256Hex(body))).thenReturn(true);

        service.process(body, sign(body));

        verify(eventRepo, never()).save(any());
        verify(accountRepo, never()).save(any());
    }

    @Test
    void subscriptionCreated_matchesByCustomData_andActivates() {
        String body = createdBody();
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);
        when(accountRepo.findByLsSubscriptionId("sub_1")).thenReturn(Optional.empty());
        BillingAccount account = new BillingAccount();
        account.setId(7L);
        account.setGroupSlug("rome");
        account.setEmail("m@x.com");
        account.setStatus(BillingStatus.PENDING_PAYMENT);
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(account));

        service.process(body, sign(body));

        assertEquals(BillingStatus.ACTIVE, account.getStatus());
        assertEquals("sub_1", account.getLsSubscriptionId());
        assertEquals("ctm_42", account.getLsCustomerId());

        ArgumentCaptor<BillingWebhookEvent> ev = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(eventRepo).save(ev.capture());
        assertTrue(ev.getValue().getProcessed());
        assertEquals(7L, ev.getValue().getMatchedAccountId());
    }

    @Test
    void unmatchedEvent_isPersistedNotDropped() {
        String body = createdBody();
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);
        when(accountRepo.findByLsSubscriptionId(any())).thenReturn(Optional.empty());
        when(accountRepo.findByGroupSlugAndEmail(any(), any())).thenReturn(Optional.empty());

        service.process(body, sign(body));

        ArgumentCaptor<BillingWebhookEvent> ev = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(eventRepo).save(ev.capture());
        assertFalse(ev.getValue().getProcessed());
        assertNull(ev.getValue().getMatchedAccountId());
        assertTrue(ev.getValue().getError().contains("unmatched"));
        verify(accountRepo, never()).save(any());
    }

    @Test
    void unparseablePayload_persistedWithError() {
        String body = "this is not json";
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);

        service.process(body, sign(body));

        ArgumentCaptor<BillingWebhookEvent> ev = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(eventRepo).save(ev.capture());
        assertFalse(ev.getValue().getProcessed());
        assertTrue(ev.getValue().getError().contains("unparseable"));
    }

    @Test
    void selfPaySupersedesGift_cancelsOldSub_adoptsNew_clearsPayer() {
        // Account currently holds a GIFT subscription (old_sub, paid by admin).
        BillingAccount account = new BillingAccount();
        account.setId(5L);
        account.setGroupSlug("rome");
        account.setEmail("m@x.com");
        account.setStatus(BillingStatus.ON_GRACE_PERIOD);
        account.setLsSubscriptionId("old_sub");
        account.setPayerEmail("admin@x.com");
        // New self-pay subscription created (payer == beneficiary).
        String body = body("subscription.created", "new_sub", "m@x.com");
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);
        when(accountRepo.findByLsSubscriptionId("new_sub")).thenReturn(Optional.empty());
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(account));

        service.process(body, sign(body));

        verify(providerClient).cancelSubscription("old_sub");
        assertEquals("new_sub", account.getLsSubscriptionId());
        assertNull(account.getPayerEmail());
        assertEquals(BillingStatus.ACTIVE, account.getStatus());
    }

    @Test
    void staleEventForSupersededSub_isIgnoredForStatus() {
        // Account already moved on to new_sub; a late cancellation for old_sub must NOT suspend it.
        BillingAccount account = new BillingAccount();
        account.setId(5L);
        account.setGroupSlug("rome");
        account.setEmail("m@x.com");
        account.setStatus(BillingStatus.ACTIVE);
        account.setLsSubscriptionId("new_sub");
        String body = body("subscription.canceled", "old_sub", "m@x.com");
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);
        when(accountRepo.findByLsSubscriptionId("old_sub")).thenReturn(Optional.empty());
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(account));

        service.process(body, sign(body));

        assertEquals(BillingStatus.ACTIVE, account.getStatus()); // unchanged
        verify(accountRepo, never()).save(any());
        ArgumentCaptor<BillingWebhookEvent> ev = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(eventRepo).save(ev.capture());
        assertTrue(ev.getValue().getProcessed());
        assertTrue(ev.getValue().getError().contains("stale"));
    }

    @Test
    void knownSubscriptionUpdate_routesBySubscriptionId() {
        BillingAccount account = new BillingAccount();
        account.setId(9L);
        account.setLsSubscriptionId("sub_1");
        account.setStatus(BillingStatus.ACTIVE);
        String body = body("subscription.past_due", "sub_1", "m@x.com");
        when(eventRepo.existsByLsEventId(any())).thenReturn(false);
        when(accountRepo.findByLsSubscriptionId("sub_1")).thenReturn(Optional.of(account));

        service.process(body, sign(body));

        assertEquals(BillingStatus.ON_GRACE_PERIOD, account.getStatus());
        verify(accountRepo).save(account);
    }

    @Test
    void mapStatus_eventTypeOverridesAndPaddleStatusMapping() {
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("transaction.payment_failed", "active"));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription.past_due", "active"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription.canceled", "active"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription.paused", "active"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("transaction.completed", "past_due"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("subscription.activated", "x"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("subscription.updated", "active"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("subscription.updated", "trialing"));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription.updated", "past_due"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription.updated", "canceled"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription.updated", "paused"));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription.updated", null));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription.updated", "weird"));
    }
}
