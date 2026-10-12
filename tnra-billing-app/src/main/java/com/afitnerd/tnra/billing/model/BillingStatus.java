package com.afitnerd.tnra.billing.model;

/**
 * Payment lifecycle of a single member's billing account.
 *
 * <pre>
 *   PENDING_PAYMENT ──pay──► ACTIVE ──renewal fails──► ON_GRACE_PERIOD ──dunning exhausted──► SUSPENDED
 *        ▲                     ▲                              │                                  │
 *        └── trial/comp lapses │                              └── card updated, charge ok ───────┘──► ACTIVE
 *                              └──────────────────────────────────────────────────────── (pay again)
 * </pre>
 *
 * ACTIVE and ON_GRACE_PERIOD are entitled (access kept while Paddle retries a failed charge).
 * PENDING_PAYMENT and SUSPENDED are not entitled (restricted to profile + pay).
 */
public enum BillingStatus {
    PENDING_PAYMENT,
    ACTIVE,
    ON_GRACE_PERIOD,
    SUSPENDED
}
