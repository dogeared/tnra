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

    /**
     * Like {@link #isEntitled(String)} but always queries the billing service (bypassing the short
     * cache) and refreshes the cache with the result. Used by the post-checkout "activating" page to
     * poll for the webhook flipping the member to ACTIVE without waiting out a stale cached {@code false}.
     */
    boolean isEntitledFresh(String email);

    /**
     * Full entitlement for an email — {@code entitled} plus the raw billing {@code status} string
     * (e.g. {@code ACTIVE}, {@code ON_GRACE_PERIOD}). Lets callers distinguish "settled" coverage from
     * dunning, which {@link #isEntitled(String)}'s boolean can't. Used by the gift flow so a member in
     * grace (failing payment) can still be gifted as a rescue.
     */
    Entitlement entitlement(String email);

    /**
     * {@code status} is the raw billing status string (empty when it couldn't be determined);
     * {@code payerEmail} is who pays for this member — null for self-pay, set when their membership is a
     * gift from that person.
     */
    record Entitlement(boolean entitled, String status, String payerEmail) {}

    /**
     * Create a hosted checkout (self-pay when payerEmail == beneficiaryEmail, else a gift).
     * {@code redirectUrl} is where Lemon Squeezy returns the buyer after payment (this app's URL).
     */
    String createCheckout(String beneficiaryEmail, String variant, String payerEmail, String redirectUrl);

    /** Hosted Customer Portal URL for a member to update their card / manage the subscription. */
    String portalUrl(String email);

    /** The accounts a payer is covering (their gift list). */
    List<CoveredMember> covering(String payerEmail);

    record CoveredMember(String email, String status) {}
}
