package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
import com.afitnerd.tnra.billing.web.dto.CoveringEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceTest {

    private BillingAccountRepository accountRepo;
    private LemonSqueezyClient lsClient;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(BillingAccountRepository.class);
        lsClient = mock(LemonSqueezyClient.class);
        service = new CheckoutService(accountRepo, lsClient);
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void selfPay_createsPendingAccountWithNullPayer_andReturnsUrl() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.empty());
        when(lsClient.createCheckout("rome", "m@x.com", "m@x.com", "monthly")).thenReturn("https://pay/1");

        String url = service.createCheckout("rome", new CheckoutRequest("M@X.com", "monthly", null));

        assertEquals("https://pay/1", url);
        ArgumentCaptor<BillingAccount> saved = ArgumentCaptor.forClass(BillingAccount.class);
        verify(accountRepo).save(saved.capture());
        assertEquals("m@x.com", saved.getValue().getEmail());
        assertNull(saved.getValue().getPayerEmail());
        assertEquals(BillingStatus.PENDING_PAYMENT, saved.getValue().getStatus());
    }

    @Test
    void gift_setsPayerEmail_andChargesPayer() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.empty());
        when(lsClient.createCheckout("rome", "ben@x.com", "admin@x.com", "yearly")).thenReturn("https://pay/2");

        String url = service.createCheckout("rome", new CheckoutRequest("ben@x.com", "yearly", "admin@x.com"));

        assertEquals("https://pay/2", url);
        ArgumentCaptor<BillingAccount> saved = ArgumentCaptor.forClass(BillingAccount.class);
        verify(accountRepo).save(saved.capture());
        assertEquals("admin@x.com", saved.getValue().getPayerEmail());
    }

    @Test
    void existingAccount_isReusedNotDuplicated() {
        BillingAccount existing = new BillingAccount();
        existing.setGroupSlug("rome");
        existing.setEmail("m@x.com");
        existing.setStatus(BillingStatus.SUSPENDED);
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(existing));
        when(lsClient.createCheckout(any(), any(), any(), any())).thenReturn("https://pay/3");

        service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null));

        verify(accountRepo).save(existing); // same row, not a new one
    }

    @Test
    void missingBeneficiary_badRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("  ", "monthly", null)));
        assertEquals(400, ex.getStatusCode().value());
        verify(lsClient, never()).createCheckout(any(), any(), any(), any());
    }

    @Test
    void missingVariant_badRequest() {
        assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("m@x.com", null, null)));
    }

    @Test
    void covering_listsGifts() {
        BillingAccount a = new BillingAccount();
        a.setEmail("ben@x.com");
        a.setStatus(BillingStatus.ACTIVE);
        when(accountRepo.findByGroupSlugAndPayerEmail("rome", "admin@x.com")).thenReturn(List.of(a));

        List<CoveringEntry> covering = service.covering("rome", "Admin@x.com");

        assertEquals(1, covering.size());
        assertEquals("ben@x.com", covering.get(0).email());
        assertEquals("ACTIVE", covering.get(0).status());
    }

    @Test
    void portal_returnsUrlWhenSubscriptionExists() {
        BillingAccount a = new BillingAccount();
        a.setLsSubscriptionId("sub_1");
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(a));
        when(lsClient.getCustomerPortalUrl("sub_1")).thenReturn("https://portal/1");

        assertEquals("https://portal/1", service.portalUrl("rome", "m@x.com"));
    }

    @Test
    void portal_notFoundWhenNoAccount() {
        when(accountRepo.findByGroupSlugAndEmail(eq("rome"), any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.portalUrl("rome", "m@x.com"));
    }

    @Test
    void portal_notFoundWhenNoSubscription() {
        BillingAccount a = new BillingAccount(); // no ls_subscription_id
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(a));
        assertThrows(ResponseStatusException.class, () -> service.portalUrl("rome", "m@x.com"));
    }
}
