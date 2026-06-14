package com.afitnerd.tnra.billing.service;

/**
 * Thin wrapper over the Lemon Squeezy REST API (there is no official Java SDK). Services depend on
 * this interface so they can be unit-tested without network calls.
 */
public interface LemonSqueezyClient {

    /**
     * Create a hosted checkout. {@code payerEmail} is who pays (prefilled + billed) — the beneficiary
     * for self-pay, the gifter for a gift. {@code groupSlug}/{@code beneficiaryEmail}/{@code payerEmail}
     * are carried in checkout custom data so the webhook can map the resulting subscription back to the
     * right beneficiary account.
     *
     * @return the hosted checkout URL to redirect the browser to.
     */
    String createCheckout(String groupSlug, String beneficiaryEmail, String payerEmail, String variant);

    /** Hosted Customer Portal URL for the payer of the given subscription (update card / cancel). */
    String getCustomerPortalUrl(String lsSubscriptionId);
}
