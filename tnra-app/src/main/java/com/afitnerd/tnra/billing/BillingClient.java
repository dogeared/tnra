package com.afitnerd.tnra.billing;

import java.util.List;

/**
 * Client the per-group app uses to talk to the central {@code tnra-billing-app}. Implemented only
 * when {@code tnra.billing.enabled=true}; when billing is off (open-source self-host) no bean exists
 * and callers treat everyone as entitled.
 */
public interface BillingClient {

    /**
     * Whether a member may use the app right now. Cached briefly and FAILS OPEN: if the billing
     * service can't be reached, returns {@code true} so a central outage never locks out paying
     * members.
     */
    boolean isEntitled(String email);

    /** Create a hosted checkout (self-pay when payerEmail == beneficiaryEmail, else a gift). */
    String createCheckout(String beneficiaryEmail, String variant, String payerEmail);

    /** Hosted Customer Portal URL for a member to update their card / manage the subscription. */
    String portalUrl(String email);

    /** The accounts a payer is covering (their gift list). */
    List<CoveredMember> covering(String payerEmail);

    record CoveredMember(String email, String status) {}
}
