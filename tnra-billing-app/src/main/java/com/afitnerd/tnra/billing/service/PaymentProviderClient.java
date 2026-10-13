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

    /** Hosted Customer Portal URL for the given provider customer (invoices / cancel / pause). */
    String getCustomerPortalUrl(String customerId);

    /**
     * Checkout URL for a payment-method update on an existing subscription — opens in the same overlay
     * as a purchase (our {@code /pay} page) and returns to the app, unlike the full-page portal. For an
     * active subscription the provider issues a zero-amount transaction; for a past_due one it returns the
     * outstanding transaction, so completing it both stores the new card and settles the failed charge.
     */
    String getUpdatePaymentMethodCheckoutUrl(String subscriptionId);

    /** Live subscription dates from the provider — paid-through, next charge, scheduled cancel, trial. */
    SubscriptionDetails getSubscription(String subscriptionId);

    /**
     * @param status            provider status (active / trialing / past_due / paused / canceled)
     * @param currentPeriodEndsAt end of the period already paid (or trial) for — "paid through"
     * @param nextBilledAt      next charge; null while trialing or when a cancel is scheduled
     * @param scheduledAction   "cancel" / "pause" / "resume" when a change is scheduled, else null
     * @param scheduledEffectiveAt when that change takes effect
     * @param trialEndsAt       trial end, when the subscription is (or was) on a trial
     * @param interval          "month" / "year"
     */
    record SubscriptionDetails(String status, java.time.Instant currentPeriodEndsAt,
                               java.time.Instant nextBilledAt, String scheduledAction,
                               java.time.Instant scheduledEffectiveAt, java.time.Instant trialEndsAt,
                               String interval) {}

    /**
     * Cancel a subscription with the provider IMMEDIATELY. Used when a beneficiary self-pays to replace a
     * gift — the prior gift subscription MUST be cancelled or the gifter keeps being charged.
     */
    void cancelSubscription(String subscriptionId);

    /**
     * Schedule a cancellation at the end of the current billing period — the member keeps what they've
     * paid for. @return when it takes effect.
     */
    java.time.Instant scheduleCancellation(String subscriptionId);

    /** Undo a scheduled cancellation (member changed their mind before the period ended). */
    void removeScheduledCancellation(String subscriptionId);
}
