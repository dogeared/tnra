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
    private PaymentProviderClient provider;
    private EntitlementService entitlementService;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        accountRepo = mock(BillingAccountRepository.class);
        provider = mock(PaymentProviderClient.class);
        entitlementService = mock(EntitlementService.class);
        service = new CheckoutService(accountRepo, provider, entitlementService);
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void beneficiaryNotEntitled(String group, String email) {
        when(entitlementService.isEntitled(group, email))
            .thenReturn(EntitlementResult.denied(BillingStatus.PENDING_PAYMENT, "NOT_ENTITLED"));
    }

    @Test
    void selfPay_createsPendingAccountWithNullPayer_andReturnsUrl() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.empty());
        when(provider.createCheckout("rome", "m@x.com", "m@x.com", "monthly", null)).thenReturn("https://pay/1");

        String url = service.createCheckout("rome", new CheckoutRequest("M@X.com", "monthly", null, null));

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
        beneficiaryNotEntitled("rome", "ben@x.com");
        when(provider.createCheckout("rome", "ben@x.com", "admin@x.com", "yearly", "https://app/back"))
            .thenReturn("https://pay/2");

        String url = service.createCheckout("rome",
            new CheckoutRequest("ben@x.com", "yearly", "admin@x.com", "https://app/back"));

        assertEquals("https://pay/2", url);
        // redirect URL is forwarded verbatim to the provider
        verify(provider).createCheckout("rome", "ben@x.com", "admin@x.com", "yearly", "https://app/back");
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
        when(provider.createCheckout(any(), any(), any(), any(), any())).thenReturn("https://pay/3");

        service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null));

        verify(accountRepo).save(existing); // same row, not a new one
    }

    @Test
    void selfPay_whenAlreadyActiveSelfPay_conflict_noNewCheckout() {
        BillingAccount active = new BillingAccount();
        active.setGroupSlug("rome");
        active.setEmail("m@x.com");
        active.setLsSubscriptionId("sub_live");
        active.setPayerEmail(null);                 // self-pay
        active.setStatus(BillingStatus.ACTIVE);
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(active));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null)));

        assertEquals(409, ex.getStatusCode().value());
        verify(provider, never()).createCheckout(any(), any(), any(), any(), any());
        verify(accountRepo, never()).save(any());
    }

    @Test
    void selfPay_whenInGraceSelfPay_conflict() {
        BillingAccount grace = new BillingAccount();
        grace.setEmail("m@x.com");
        grace.setLsSubscriptionId("sub_live");
        grace.setStatus(BillingStatus.ON_GRACE_PERIOD);
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(grace));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void gift_blockedWhenBeneficiaryAlreadyEntitled_noCheckout() {
        // Beneficiary is already covered (any reason) — a gift would charge the payer for nothing.
        when(entitlementService.isEntitled("rome", "ben@x.com"))
            .thenReturn(EntitlementResult.entitled(BillingStatus.ACTIVE, "SUBSCRIPTION"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome",
                new CheckoutRequest("ben@x.com", "yearly", "admin@x.com", null)));

        assertEquals(409, ex.getStatusCode().value());
        verify(provider, never()).createCheckout(any(), any(), any(), any(), any());
        verify(accountRepo, never()).save(any());
    }

    @Test
    void gift_allowedWhenBeneficiaryOnGracePeriod_rescue() {
        // Dunning (failing own payment) is giftable — the gift rescues/supersedes the failing sub.
        when(entitlementService.isEntitled("rome", "ben@x.com"))
            .thenReturn(EntitlementResult.entitled(BillingStatus.ON_GRACE_PERIOD, "SUBSCRIPTION"));
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.empty());
        when(provider.createCheckout(any(), any(), any(), any(), any())).thenReturn("https://pay/rescue");

        String url = service.createCheckout("rome",
            new CheckoutRequest("ben@x.com", "monthly", "admin@x.com", null));

        assertEquals("https://pay/rescue", url);
    }

    @Test
    void gift_allowedWhenBeneficiaryNotEntitled() {
        beneficiaryNotEntitled("rome", "ben@x.com");
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.empty());
        when(provider.createCheckout(any(), any(), any(), any(), any())).thenReturn("https://pay/g");

        String url = service.createCheckout("rome",
            new CheckoutRequest("ben@x.com", "yearly", "admin@x.com", null));

        assertEquals("https://pay/g", url);
    }

    @Test
    void selfPay_whenSuspended_isAllowed_canResubscribe() {
        BillingAccount suspended = new BillingAccount();
        suspended.setEmail("m@x.com");
        suspended.setLsSubscriptionId("sub_old");
        suspended.setStatus(BillingStatus.SUSPENDED);
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(suspended));
        when(provider.createCheckout(any(), any(), any(), any(), any())).thenReturn("https://pay/r");

        assertEquals("https://pay/r",
            service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null)));
    }

    @Test
    void missingBeneficiary_badRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("  ", "monthly", null, null)));
        assertEquals(400, ex.getStatusCode().value());
        verify(provider, never()).createCheckout(any(), any(), any(), any(), any());
    }

    @Test
    void missingVariant_badRequest() {
        assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("m@x.com", null, null, null)));
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
    void portal_returnsUrlWhenCustomerExists() {
        BillingAccount a = new BillingAccount();
        a.setLsCustomerId("ctm_1");
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(a));
        when(provider.getCustomerPortalUrl("ctm_1")).thenReturn("https://portal/1");

        assertEquals("https://portal/1", service.portalUrl("rome", "m@x.com"));
    }

    @Test
    void portal_notFoundWhenNoAccount() {
        when(accountRepo.findByGroupSlugAndEmail(eq("rome"), any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.portalUrl("rome", "m@x.com"));
    }

    @Test
    void portal_notFoundWhenNoSubscription() {
        BillingAccount a = new BillingAccount(); // no ls_subscription_id, and not covering anyone
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(a));
        assertThrows(ResponseStatusException.class, () -> service.portalUrl("rome", "m@x.com"));
    }

    @Test
    void portal_fallsBackToGiftSubscription_forPureGifter() {
        // The member has no membership of their own, but pays for someone else's (a gift).
        when(accountRepo.findByGroupSlugAndEmail("rome", "gifter@x.com")).thenReturn(Optional.empty());
        BillingAccount gift = new BillingAccount();
        gift.setEmail("ben@x.com");
        gift.setPayerEmail("gifter@x.com");
        gift.setLsCustomerId("ctm_gift");
        when(accountRepo.findByGroupSlugAndPayerEmail("rome", "gifter@x.com")).thenReturn(List.of(gift));
        when(provider.getCustomerPortalUrl("ctm_gift")).thenReturn("https://portal/gift");

        assertEquals("https://portal/gift", service.portalUrl("rome", "gifter@x.com"));
    }

    @Test
    void portal_prefersOwnCustomerOverGift() {
        BillingAccount own = new BillingAccount();
        own.setEmail("gifter@x.com");
        own.setLsCustomerId("ctm_own");
        when(accountRepo.findByGroupSlugAndEmail("rome", "gifter@x.com")).thenReturn(Optional.of(own));
        when(provider.getCustomerPortalUrl("ctm_own")).thenReturn("https://portal/own");

        assertEquals("https://portal/own", service.portalUrl("rome", "gifter@x.com"));
        verify(accountRepo, never()).findByGroupSlugAndPayerEmail(any(), any());
    }
}
