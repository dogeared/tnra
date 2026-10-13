package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.service.CheckoutService;
import com.afitnerd.tnra.billing.service.EntitlementResult;
import com.afitnerd.tnra.billing.service.EntitlementService;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
import com.afitnerd.tnra.billing.web.dto.CancelResponse;
import com.afitnerd.tnra.billing.web.dto.MemberRequest;
import com.afitnerd.tnra.billing.web.dto.StopCoveringRequest;
import com.afitnerd.tnra.billing.web.dto.CheckoutResponse;
import com.afitnerd.tnra.billing.web.dto.SubscriptionResponse;
import com.afitnerd.tnra.billing.web.dto.CoveringEntry;
import com.afitnerd.tnra.billing.web.dto.EntitlementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Per-group billing API. The caller is authenticated by its per-group bearer token (see
 * {@code GroupTokenAuthFilter}); {@code principal.getName()} is the caller's group_slug, so every
 * operation is implicitly scoped to that group and a caller can never read or mutate another group.
 *
 * Identity note: the group app is responsible for setting beneficiary/payer emails from trusted OIDC
 * identity (the self-pay identity guard lives there) — this service trusts the token-authenticated caller.
 */
@RestController
@RequestMapping("/api/v1")
public class BillingApiController {

    private final EntitlementService entitlementService;
    private final CheckoutService checkoutService;

    public BillingApiController(EntitlementService entitlementService, CheckoutService checkoutService) {
        this.entitlementService = entitlementService;
        this.checkoutService = checkoutService;
    }

    @GetMapping("/entitlement")
    public EntitlementResponse entitlement(Principal principal, @RequestParam String email) {
        EntitlementResult result = entitlementService.isEntitled(principal.getName(), email);
        return new EntitlementResponse(result.entitled(), result.status().name(), result.reason(),
            result.payerEmail());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(Principal principal, @RequestBody CheckoutRequest request) {
        return new CheckoutResponse(checkoutService.createCheckout(principal.getName(), request));
    }

    @GetMapping("/covering")
    public List<CoveringEntry> covering(Principal principal, @RequestParam String payerEmail) {
        return checkoutService.covering(principal.getName(), payerEmail);
    }

    @GetMapping("/portal")
    public CheckoutResponse portal(Principal principal, @RequestParam String email) {
        return new CheckoutResponse(checkoutService.portalUrl(principal.getName(), email));
    }

    /** Member cancels their own membership at period end (in-app; never the provider portal). */
    @PostMapping("/cancel")
    public CancelResponse cancel(Principal principal, @RequestBody MemberRequest request) {
        return new CancelResponse(checkoutService.cancelOwn(principal.getName(), request.email()));
    }

    /** Undo a scheduled cancellation before the period ends. */
    @PostMapping("/resume")
    public void resume(Principal principal, @RequestBody MemberRequest request) {
        checkoutService.resumeOwn(principal.getName(), request.email());
    }

    /** A payer stops covering one member at period end. */
    @PostMapping("/covering/cancel")
    public CancelResponse stopCovering(Principal principal, @RequestBody StopCoveringRequest request) {
        return new CancelResponse(checkoutService.stopCovering(principal.getName(),
            request.payerEmail(), request.beneficiaryEmail()));
    }

    /** Live subscription dates for the member's billing screen (provider call — fetch lazily). */
    @GetMapping("/subscription")
    public SubscriptionResponse subscription(Principal principal, @RequestParam String email) {
        return checkoutService.subscriptionDetails(principal.getName(), email);
    }

    /** Overlay checkout URL to update the card on the member's own subscription (see CheckoutService). */
    @GetMapping("/payment-method")
    public CheckoutResponse paymentMethod(Principal principal, @RequestParam String email) {
        return new CheckoutResponse(checkoutService.paymentMethodUpdateUrl(principal.getName(), email));
    }
}
