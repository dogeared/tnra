package com.afitnerd.tnra.billing.web.dto;

import java.time.Instant;

/**
 * Live subscription facts for the member's own billing screen (fetched from the provider on demand —
 * NOT on every entitlement check). All instants may be null depending on state.
 *
 * @param status         our entitlement status (ACTIVE / ON_GRACE_PERIOD / …)
 * @param providerStatus Paddle's (active / trialing / past_due / paused / canceled)
 * @param variant        "monthly" / "yearly" as recorded at checkout
 * @param payerEmail     the gifter, or null for self-pay
 * @param paidThrough    end of the period already paid (or trial) for
 * @param nextBilledAt   next charge; null while trialing or when a cancel is scheduled
 * @param cancelAt       when a scheduled cancellation takes effect; null if none
 * @param trialEndsAt    trial end if on a trial
 */
public record SubscriptionResponse(String status, String providerStatus, String variant, String payerEmail,
                                   Instant paidThrough, Instant nextBilledAt, Instant cancelAt,
                                   Instant trialEndsAt) {}
