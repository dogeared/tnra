package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
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
 *     ─► Lemon Squeezy hosted checkout URL (payer is charged)
 * </pre>
 *
 * The redirect/webhook race is benign: the account row exists (PENDING) before the URL is returned,
 * so a redirect-back-before-webhook reads PENDING and the gate shows "payment processing" until the
 * webhook flips it to ACTIVE.
 */
@Service
public class CheckoutService {

    private final BillingAccountRepository accountRepository;
    private final LemonSqueezyClient lemonSqueezyClient;
    private final EntitlementService entitlementService;

    public CheckoutService(BillingAccountRepository accountRepository,
                           LemonSqueezyClient lemonSqueezyClient,
                           EntitlementService entitlementService) {
        this.accountRepository = accountRepository;
        this.lemonSqueezyClient = lemonSqueezyClient;
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

        return lemonSqueezyClient.createCheckout(groupSlug, beneficiary, payer, variant,
            request.redirectUrl());
    }

    public List<CoveringEntry> covering(String groupSlug, String payerEmail) {
        String payer = normalize(payerEmail);
        return accountRepository.findByGroupSlugAndPayerEmail(groupSlug, payer).stream()
            .map(a -> new CoveringEntry(a.getEmail(), a.getStatus().name()))
            .toList();
    }

    /**
     * Hosted Customer Portal link. Prefers the member's OWN subscription; if they have none (e.g. a
     * trial/comp member who has only gifted memberships to others), falls back to any subscription they
     * PAY for. All of a payer's subscriptions — their own and any gifts — share one Lemon Squeezy
     * customer (the payer's email), so any of them opens the same portal listing every one of them.
     */
    public String portalUrl(String groupSlug, String email) {
        String normalized = normalize(email);
        String subscriptionId = accountRepository.findByGroupSlugAndEmail(groupSlug, normalized)
            .map(BillingAccount::getLsSubscriptionId)
            .filter(Objects::nonNull)
            .orElseGet(() -> firstPaidSubscriptionId(groupSlug, normalized));
        if (subscriptionId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription to manage");
        }
        return lemonSqueezyClient.getCustomerPortalUrl(subscriptionId);
    }

    /** Any subscription this member pays for (a gift) — used to reach the portal when they have no own sub. */
    private String firstPaidSubscriptionId(String groupSlug, String payer) {
        return accountRepository.findByGroupSlugAndPayerEmail(groupSlug, payer).stream()
            .map(BillingAccount::getLsSubscriptionId)
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
