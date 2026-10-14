package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
import com.afitnerd.tnra.billing.web.dto.SubscriptionResponse;
import com.afitnerd.tnra.billing.web.dto.CoveringEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void selfPay_whenProviderReportsALiveSubscription_conflict_noCheckout_noSave() {
        // Local DB lags (PENDING), but Paddle already bills this member: block the second charge.
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.empty());
        when(provider.hasLiveSubscription("rome", "m@x.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null)));

        assertEquals(409, ex.getStatusCode().value());
        verify(provider, never()).createCheckout(any(), any(), any(), any(), any());
        verify(accountRepo, never()).save(any());
    }

    @Test
    void selfPay_whenProviderLookupFails_failsOpen_andCreatesTheCheckout() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.empty());
        when(provider.hasLiveSubscription("rome", "m@x.com")).thenThrow(new IllegalStateException("paddle down"));
        when(provider.createCheckout("rome", "m@x.com", "m@x.com", "monthly", null)).thenReturn("https://pay/ok");

        assertEquals("https://pay/ok",
            service.createCheckout("rome", new CheckoutRequest("m@x.com", "monthly", null, null)));
    }

    @Test
    void gift_doesNotConsultTheProviderGuard() {
        beneficiaryNotEntitled("rome", "ben@x.com");
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.empty());
        when(provider.createCheckout("rome", "ben@x.com", "payer@x.com", "monthly", null)).thenReturn("https://pay/g");

        service.createCheckout("rome", new CheckoutRequest("ben@x.com", "monthly", "payer@x.com", null));

        verify(provider, never()).hasLiveSubscription(any(), any());
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

    private BillingAccount accountWithSub(String email, String payer, String sub) {
        BillingAccount a = new BillingAccount();
        a.setGroupSlug("rome");
        a.setEmail(email);
        a.setPayerEmail(payer);
        a.setLsSubscriptionId(sub);
        a.setStatus(BillingStatus.ACTIVE);
        return a;
    }

    @Test
    void paymentMethodUpdateUrl_selfPay_returnsOverlayCheckoutUrl() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com"))
            .thenReturn(Optional.of(accountWithSub("m@x.com", null, "sub_1")));
        when(provider.getUpdatePaymentMethodCheckoutUrl("sub_1")).thenReturn("https://tnra.app/pay?_ptxn=txn_u");

        assertEquals("https://tnra.app/pay?_ptxn=txn_u", service.paymentMethodUpdateUrl("rome", " M@X.com "));
    }

    @Test
    void paymentMethodUpdateUrl_giftedMember_isConflict_namingThePayer() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com"))
            .thenReturn(Optional.of(accountWithSub("ben@x.com", "admin@x.com", "sub_2")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.paymentMethodUpdateUrl("rome", "ben@x.com"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("admin@x.com"));
        verify(provider, never()).getUpdatePaymentMethodCheckoutUrl(any());
    }

    @Test
    void paymentMethodUpdateUrl_noAccountOrNoSubscription_isNotFound() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "none@x.com")).thenReturn(Optional.empty());
        ResponseStatusException none = assertThrows(ResponseStatusException.class,
            () -> service.paymentMethodUpdateUrl("rome", "none@x.com"));
        assertEquals(HttpStatus.NOT_FOUND, none.getStatusCode());

        when(accountRepo.findByGroupSlugAndEmail("rome", "pending@x.com"))
            .thenReturn(Optional.of(accountWithSub("pending@x.com", null, null)));
        ResponseStatusException noSub = assertThrows(ResponseStatusException.class,
            () -> service.paymentMethodUpdateUrl("rome", "pending@x.com"));
        assertEquals(HttpStatus.NOT_FOUND, noSub.getStatusCode());
    }

    @Test
    void subscriptionDetails_mapsProviderDates_andScheduledCancelHidesNextCharge() {
        BillingAccount a = accountWithSub("m@x.com", null, "sub_1");
        a.setLsVariant("monthly");
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(a));
        java.time.Instant end = java.time.Instant.parse("2026-12-12T20:03:10Z");
        when(provider.getSubscription("sub_1")).thenReturn(new PaymentProviderClient.SubscriptionDetails(
            "trialing", end, java.time.Instant.parse("2026-12-13T00:00:00Z"), "cancel", end, end, "month"));

        SubscriptionResponse r = service.subscriptionDetails("rome", "M@x.com");

        assertEquals("ACTIVE", r.status());
        assertEquals("trialing", r.providerStatus());
        assertEquals("monthly", r.variant());
        assertNull(r.payerEmail());
        assertEquals(end, r.paidThrough());
        assertNull(r.nextBilledAt(), "a scheduled cancel means no further charge");
        assertEquals(end, r.cancelAt());
        assertEquals(end, r.trialEndsAt());
    }

    @Test
    void subscriptionDetails_noScheduledChange_keepsNextCharge_andPayer() {
        BillingAccount a = accountWithSub("ben@x.com", "admin@x.com", "sub_2");
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.of(a));
        java.time.Instant next = java.time.Instant.parse("2026-11-13T00:00:00Z");
        when(provider.getSubscription("sub_2")).thenReturn(new PaymentProviderClient.SubscriptionDetails(
            "active", next, next, null, null, null, "month"));

        SubscriptionResponse r = service.subscriptionDetails("rome", "ben@x.com");

        assertEquals("admin@x.com", r.payerEmail());
        assertEquals(next, r.nextBilledAt());
        assertNull(r.cancelAt());
    }

    @Test
    void subscriptionDetails_noSubscription_isNotFound() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "none@x.com")).thenReturn(Optional.empty());
        when(accountRepo.findByGroupSlugAndEmail("rome", "pending@x.com"))
            .thenReturn(Optional.of(accountWithSub("pending@x.com", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.subscriptionDetails("rome", "none@x.com")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.subscriptionDetails("rome", "pending@x.com")).getStatusCode());
        verify(provider, never()).getSubscription(any());
    }

    // ---- in-app cancellation ----

    @Test
    void cancelOwn_selfPay_schedulesAtPeriodEnd() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com"))
            .thenReturn(Optional.of(accountWithSub("m@x.com", null, "sub_1")));
        java.time.Instant end = java.time.Instant.parse("2026-12-12T00:00:00Z");
        when(provider.scheduleCancellation("sub_1")).thenReturn(end);

        assertEquals(end, service.cancelOwn("rome", "M@x.com"));
        verify(provider, never()).cancelSubscription(any()); // never the immediate cancel
    }

    @Test
    void cancelOwn_andResumeOwn_refuseAGiftedMembership() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com"))
            .thenReturn(Optional.of(accountWithSub("ben@x.com", "admin@x.com", "sub_2")));

        ResponseStatusException c = assertThrows(ResponseStatusException.class, () -> service.cancelOwn("rome", "ben@x.com"));
        assertEquals(HttpStatus.CONFLICT, c.getStatusCode());
        ResponseStatusException r = assertThrows(ResponseStatusException.class, () -> service.resumeOwn("rome", "ben@x.com"));
        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        verify(provider, never()).scheduleCancellation(any());
        verify(provider, never()).removeScheduledCancellation(any());
    }

    @Test
    void resumeOwn_removesScheduledChange() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com"))
            .thenReturn(Optional.of(accountWithSub("m@x.com", null, "sub_1")));

        service.resumeOwn("rome", "m@x.com");

        verify(provider).removeScheduledCancellation("sub_1");
    }

    @Test
    void stopCovering_onlyThePayer_mayEndAGift() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com"))
            .thenReturn(Optional.of(accountWithSub("ben@x.com", "admin@x.com", "sub_2")));
        java.time.Instant end = java.time.Instant.parse("2026-12-12T00:00:00Z");
        when(provider.scheduleCancellation("sub_2")).thenReturn(end);

        assertEquals(end, service.stopCovering("rome", "Admin@x.com", "ben@x.com"));

        ResponseStatusException f = assertThrows(ResponseStatusException.class,
            () -> service.stopCovering("rome", "someone-else@x.com", "ben@x.com"));
        assertEquals(HttpStatus.FORBIDDEN, f.getStatusCode());
        verify(provider, org.mockito.Mockito.times(1)).scheduleCancellation("sub_2");
    }

    @Test
    void stopCovering_noSubscription_isNotFound() {
        when(accountRepo.findByGroupSlugAndEmail("rome", "none@x.com")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> service.stopCovering("rome", "admin@x.com", "none@x.com")).getStatusCode());
    }

    @Test
    void covering_includesScheduledCancelDates_andToleratesProviderErrors() {
        BillingAccount a = accountWithSub("a@x.com", "admin@x.com", "sub_a");
        BillingAccount b = accountWithSub("b@x.com", "admin@x.com", "sub_b");
        BillingAccount c = accountWithSub("c@x.com", "admin@x.com", null);
        when(accountRepo.findByGroupSlugAndPayerEmail("rome", "admin@x.com")).thenReturn(List.of(a, b, c));
        java.time.Instant end = java.time.Instant.parse("2026-12-12T00:00:00Z");
        when(provider.getSubscription("sub_a")).thenReturn(new PaymentProviderClient.SubscriptionDetails("active", end, null, "cancel", end, null, "month"));
        when(provider.getSubscription("sub_b")).thenThrow(new RuntimeException("paddle down"));

        List<CoveringEntry> list = service.covering("rome", "admin@x.com");

        assertEquals(3, list.size());
        assertEquals(end, list.get(0).cancelAt());
        assertNull(list.get(1).cancelAt());
        assertNull(list.get(2).cancelAt());
        verify(provider, never()).getSubscription(null);
    }

    // ---- portal session must be the member's OWN customer ----

    @Test
    void portalUrl_giftedMember_neverGetsTheGiftersCustomerSession() {
        BillingAccount gifted = accountWithSub("ben@x.com", "admin@x.com", "sub_2");
        gifted.setLsCustomerId("ctm_admin"); // the gifter's Paddle customer, recorded from the webhook
        when(accountRepo.findByGroupSlugAndEmail("rome", "ben@x.com")).thenReturn(Optional.of(gifted));
        when(accountRepo.findByGroupSlugAndPayerEmail("rome", "ben@x.com")).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.portalUrl("rome", "ben@x.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(provider, never()).getCustomerPortalUrl("ctm_admin");
    }

    @Test
    void portalUrl_selfPayMember_getsTheirOwnCustomer() {
        BillingAccount own = accountWithSub("m@x.com", null, "sub_1");
        own.setLsCustomerId("ctm_me");
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.of(own));
        when(provider.getCustomerPortalUrl("ctm_me")).thenReturn("https://portal/me");

        assertEquals("https://portal/me", service.portalUrl("rome", "m@x.com"));
    }
}
