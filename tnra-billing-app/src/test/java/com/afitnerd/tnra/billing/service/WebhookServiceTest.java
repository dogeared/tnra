package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.LemonSqueezyProperties;
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
    private LemonSqueezyProperties props;
    private WebhookService service;

    @BeforeEach
    void setUp() {
        eventRepo = mock(BillingWebhookEventRepository.class);
        accountRepo = mock(BillingAccountRepository.class);
        props = new LemonSqueezyProperties();
        props.setWebhookSecret(SECRET);
        service = new WebhookService(eventRepo, accountRepo, props, new ObjectMapper());
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private String sign(String body) {
        return HashUtil.hmacSha256Hex(body, SECRET);
    }

    private String createdBody() {
        return "{\"meta\":{\"event_name\":\"subscription_created\","
            + "\"custom_data\":{\"group_slug\":\"rome\",\"beneficiary_email\":\"m@x.com\",\"payer_email\":\"m@x.com\"}},"
            + "\"data\":{\"id\":\"sub_1\",\"attributes\":{\"status\":\"active\",\"customer_id\":42}}}";
    }

    @Test
    void invalidSignature_unauthorized_andNothingPersisted() {
        String body = createdBody();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.process(body, "deadbeef"));
        assertEquals(401, ex.getStatusCode().value());
        verify(eventRepo, never()).save(any());
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
        assertEquals("42", account.getLsCustomerId());

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
    void mapStatus_eventNameOverridesAndLsStatusMapping() {
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription_payment_failed", "active"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription_expired", "active"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription_cancelled", "active"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("subscription_updated", "active"));
        assertEquals(BillingStatus.ACTIVE, service.mapStatus("subscription_updated", "on_trial"));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription_updated", "past_due"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription_updated", "unpaid"));
        assertEquals(BillingStatus.SUSPENDED, service.mapStatus("subscription_updated", "paused"));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription_updated", null));
        assertEquals(BillingStatus.ON_GRACE_PERIOD, service.mapStatus("subscription_updated", "weird"));
    }
}
