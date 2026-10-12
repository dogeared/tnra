package com.afitnerd.tnra.billing.service;

/**
 * Thin wrapper over a payment provider's REST API (currently Paddle; previously Lemon Squeezy).
 * Services depend on this interface so they can be unit-tested without network calls and so the
 * provider can be swapped without touching the entitlement core.
 */
public interface PaymentProviderClient {

    /**
     * Create a hosted checkout. {@code payerEmail} is who pays (prefilled + billed) — the beneficiary
     * for self-pay, the gifter for a gift. {@code groupSlug}/{@code beneficiaryEmail}/{@code payerEmail}
     * are carried in checkout custom data so the webhook can map the resulting subscription back to the
     * right beneficiary account. {@code redirectUrl} (null/blank to skip) is where the provider returns
     * the buyer after payment.
     *
     * @return the hosted checkout URL to redirect the browser to.
     */
    String createCheckout(String groupSlug, String beneficiaryEmail, String payerEmail, String variant,
                          String redirectUrl);

    /** Hosted Customer Portal URL for the given provider customer (update card / cancel). */
    String getCustomerPortalUrl(String customerId);

    /**
     * Cancel a subscription with the provider. Used when a beneficiary self-pays to replace a gift —
     * the prior gift subscription MUST be cancelled or the gifter keeps being charged.
     */
    void cancelSubscription(String subscriptionId);
}
