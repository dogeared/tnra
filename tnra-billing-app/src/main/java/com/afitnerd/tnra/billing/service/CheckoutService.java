package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
import com.afitnerd.tnra.billing.web.dto.SubscriptionResponse;
import com.afitnerd.tnra.billing.web.dto.CoveringEntry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Checkout, gift, "covering" listing, and portal lookups. All scoped to a single group (the caller
 * is authenticated by its per-group token, which fixes group_slug).
 *
 * <pre>
 *   createCheckout
 *     normalize emails ─► payer = payerEmail or beneficiary (self-pay)
 *     find-or-create beneficiary account (dedup on group+email; PENDING if new)
 *     set payer_email (null for self-pay, the gifter for a gift) + variant
 *     ─► Paddle hosted checkout URL (payer is charged)
 * </pre>
 *
 * The redirect/webhook race is benign: the account row exists (PENDING) before the URL is returned,
 * so a redirect-back-before-webhook reads PENDING and the gate shows "payment processing" until the
 * webhook flips it to ACTIVE.
 */
@Service
public class CheckoutService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CheckoutService.class);

    private final BillingAccountRepository accountRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final EntitlementService entitlementService;

    public CheckoutService(BillingAccountRepository accountRepository,
                           PaymentProviderClient paymentProviderClient,
                           EntitlementService entitlementService) {
        this.accountRepository = accountRepository;
        this.paymentProviderClient = paymentProviderClient;
        this.entitlementService = entitlementService;
    }

    public String createCheckout(String groupSlug, CheckoutRequest request) {
        String beneficiary = normalize(request.beneficiaryEmail());
        String variant = request.variant() == null ? "" : request.variant().trim();
        if (beneficiary.isEmpty() || variant.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "beneficiaryEmail and variant are required");
        }
        String payer = normalize(request.payerEmail());
        if (payer.isEmpty()) {
            payer = beneficiary; // self-pay
        }
        boolean gift = !payer.equalsIgnoreCase(beneficiary);

        // Don't let a gift through for a member who is already SETTLED — active subscription, trial,
        // exempt, or an existing gift — the payer would be charged for nothing. A member in dunning
        // (ON_GRACE_PERIOD: their own payment is failing) is intentionally still giftable: the gift is a
        // rescue that supersedes the failing subscription. Entitlement is the source of truth here, so
        // trial/exempt coverage counts too.
        if (gift) {
            EntitlementResult ben = entitlementService.isEntitled(groupSlug, beneficiary);
            if (ben.entitled() && ben.status() != BillingStatus.ON_GRACE_PERIOD) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This member already has an active membership — no gift needed.");
            }
        }

        BillingAccount account = accountRepository
            .findByGroupSlugAndEmail(groupSlug, beneficiary)
            .orElseGet(() -> newAccount(groupSlug, beneficiary));

        // Prevent a double charge: a member who already self-pays a live subscription must not start a
        // second one — direct them to the customer portal instead. A GIFT checkout is intentionally NOT
        // blocked (that's the legitimate "self-pay takes over a gift" supersede path, where the existing
        // account is gift-covered, i.e. payer_email is set).
        if (!gift && hasLiveSelfPaySubscription(account)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This member already has an active subscription. "
                    + "Use \"Update payment method\" to manage it.");
        }

        account.setPayerEmail(gift ? payer : null);
        account.setLsVariant(variant);
        accountRepository.save(account);

        return paymentProviderClient.createCheckout(groupSlug, beneficiary, payer, variant,
            request.redirectUrl());
    }

    public List<CoveringEntry> covering(String groupSlug, String payerEmail) {
        String payer = normalize(payerEmail);
        return accountRepository.findByGroupSlugAndPayerEmail(groupSlug, payer).stream()
            .map(a -> new CoveringEntry(a.getEmail(), a.getStatus().name(), scheduledCancelAt(a)))
            .toList();
    }

    /** Live from the provider; null when unknown. Only called from billing screens (lazy), never the gate. */
    private java.time.Instant scheduledCancelAt(BillingAccount a) {
        if (a.getLsSubscriptionId() == null) {
            return null;
        }
        try {
            PaymentProviderClient.SubscriptionDetails d = paymentProviderClient.getSubscription(a.getLsSubscriptionId());
            return "cancel".equalsIgnoreCase(d.scheduledAction()) ? d.scheduledEffectiveAt() : null;
        } catch (Exception e) {
            log.warn("Couldn't read subscription {} for covering list: {}", a.getLsSubscriptionId(), e.getMessage());
            return null;
        }
    }

    /**
     * Member cancels their OWN membership at the end of the paid period — in the app, never via the
     * provider portal (where several identical rows make the wrong one easy to pick). A gifted membership
     * is the payer's to cancel: 409.
     */
    public java.time.Instant cancelOwn(String groupSlug, String email) {
        BillingAccount account = ownSubscription(groupSlug, email);
        return paymentProviderClient.scheduleCancellation(account.getLsSubscriptionId());
    }

    /** Undo {@link #cancelOwn} before the period ends. */
    public void resumeOwn(String groupSlug, String email) {
        BillingAccount account = ownSubscription(groupSlug, email);
        paymentProviderClient.removeScheduledCancellation(account.getLsSubscriptionId());
    }

    /**
     * A payer stops covering one specific member at the end of the paid period. The payer is checked
     * against the account's payer_email — only the gifter may end a gift.
     */
    public java.time.Instant stopCovering(String groupSlug, String payerEmail, String beneficiaryEmail) {
        String payer = normalize(payerEmail);
        BillingAccount account = accountRepository.findByGroupSlugAndEmail(groupSlug, normalize(beneficiaryEmail))
            .filter(a -> a.getLsSubscriptionId() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No gift subscription for that member"));
        if (account.getPayerEmail() == null || !account.getPayerEmail().equalsIgnoreCase(payer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not paying for that membership");
        }
        return paymentProviderClient.scheduleCancellation(account.getLsSubscriptionId());
    }

    private BillingAccount ownSubscription(String groupSlug, String email) {
        String normalized = normalize(email);
        BillingAccount account = accountRepository.findByGroupSlugAndEmail(groupSlug, normalized)
            .filter(a -> a.getLsSubscriptionId() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription"));
        if (account.getPayerEmail() != null && !account.getPayerEmail().equalsIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This membership is paid for by " + account.getPayerEmail() + ", who manages it");
        }
        return account;
    }

    /**
     * Hosted Customer Portal link. Prefers the member's OWN provider customer; if they have none (e.g. a
     * trial/comp member who has only gifted memberships to others), falls back to any account they PAY
     * for. All of a payer's subscriptions — their own and any gifts — share one Paddle customer (the
     * payer), so that customer's portal lists every subscription they pay for.
     */
    public String portalUrl(String groupSlug, String email) {
        String normalized = normalize(email);
        // SECURITY: the customer id on a GIFTED account is the gifter's Paddle customer (the gift was
        // bought under their email). A portal session for it would let the recipient see — and cancel —
        // every subscription the gifter pays for, including the gifter's own. Only a self-paid account's
        // customer is the member's own; otherwise fall back to subscriptions THEY pay for, or nothing.
        String customerId = accountRepository.findByGroupSlugAndEmail(groupSlug, normalized)
            .filter(a -> a.getPayerEmail() == null || a.getPayerEmail().equalsIgnoreCase(normalized))
            .map(BillingAccount::getLsCustomerId)
            .filter(Objects::nonNull)
            .orElseGet(() -> firstPaidCustomerId(groupSlug, normalized));
        if (customerId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription to manage");
        }
        return paymentProviderClient.getCustomerPortalUrl(customerId);
    }

    /**
     * Checkout URL to update the card on the member's OWN subscription (overlay flow). A gifted member has
     * no card of their own on file — the gifter manages it — so that is a 409 with a message the UI can show.
     */
    public String paymentMethodUpdateUrl(String groupSlug, String email) {
        String normalized = normalize(email);
        BillingAccount account = accountRepository.findByGroupSlugAndEmail(groupSlug, normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription to update"));
        if (account.getLsSubscriptionId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription to update");
        }
        if (account.getPayerEmail() != null && !account.getPayerEmail().equalsIgnoreCase(normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This membership is paid for by " + account.getPayerEmail() + ", who manages the payment method");
        }
        return paymentProviderClient.getUpdatePaymentMethodCheckoutUrl(account.getLsSubscriptionId());
    }

    /** Live dates for the member's subscription (own or gifted); 404 when they have none. */
    public SubscriptionResponse subscriptionDetails(String groupSlug, String email) {
        String normalized = normalize(email);
        BillingAccount account = accountRepository.findByGroupSlugAndEmail(groupSlug, normalized)
            .filter(a -> a.getLsSubscriptionId() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription"));
        PaymentProviderClient.SubscriptionDetails d = paymentProviderClient.getSubscription(account.getLsSubscriptionId());
        boolean cancelScheduled = "cancel".equalsIgnoreCase(d.scheduledAction()) && d.scheduledEffectiveAt() != null;
        return new SubscriptionResponse(
            account.getStatus() == null ? null : account.getStatus().name(),
            d.status(), account.getLsVariant(), account.getPayerEmail(),
            d.currentPeriodEndsAt(),
            cancelScheduled ? null : d.nextBilledAt(),
            cancelScheduled ? d.scheduledEffectiveAt() : null,
            d.trialEndsAt());
    }

    /** The provider customer of any subscription this member pays for — to reach the portal with no own sub. */
    private String firstPaidCustomerId(String groupSlug, String payer) {
        return accountRepository.findByGroupSlugAndPayerEmail(groupSlug, payer).stream()
            .map(BillingAccount::getLsCustomerId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /** A member already paying for themselves: a real LS subscription, self-pay, and not lapsed. */
    private boolean hasLiveSelfPaySubscription(BillingAccount account) {
        return account.getLsSubscriptionId() != null
            && account.getPayerEmail() == null
            && (account.getStatus() == BillingStatus.ACTIVE
                || account.getStatus() == BillingStatus.ON_GRACE_PERIOD);
    }

    private BillingAccount newAccount(String groupSlug, String beneficiary) {
        BillingAccount a = new BillingAccount();
        a.setGroupSlug(groupSlug);
        a.setEmail(beneficiary);
        a.setStatus(BillingStatus.PENDING_PAYMENT);
        return a;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
