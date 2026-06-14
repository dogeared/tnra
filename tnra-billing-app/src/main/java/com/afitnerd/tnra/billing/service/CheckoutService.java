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

    public CheckoutService(BillingAccountRepository accountRepository,
                           LemonSqueezyClient lemonSqueezyClient) {
        this.accountRepository = accountRepository;
        this.lemonSqueezyClient = lemonSqueezyClient;
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

        BillingAccount account = accountRepository
            .findByGroupSlugAndEmail(groupSlug, beneficiary)
            .orElseGet(() -> newAccount(groupSlug, beneficiary));
        account.setPayerEmail(gift ? payer : null);
        account.setLsVariant(variant);
        accountRepository.save(account);

        return lemonSqueezyClient.createCheckout(groupSlug, beneficiary, payer, variant);
    }

    public List<CoveringEntry> covering(String groupSlug, String payerEmail) {
        String payer = normalize(payerEmail);
        return accountRepository.findByGroupSlugAndPayerEmail(groupSlug, payer).stream()
            .map(a -> new CoveringEntry(a.getEmail(), a.getStatus().name()))
            .toList();
    }

    public String portalUrl(String groupSlug, String email) {
        BillingAccount account = accountRepository
            .findByGroupSlugAndEmail(groupSlug, normalize(email))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No billing account"));
        if (account.getLsSubscriptionId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription to manage");
        }
        return lemonSqueezyClient.getCustomerPortalUrl(account.getLsSubscriptionId());
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
