package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingStatus;

/**
 * Outcome of an entitlement check. {@code status} is the member's current billing status
 * (PENDING_PAYMENT when there's no account yet); {@code reason} names which rule decided it.
 */
public record EntitlementResult(boolean entitled, BillingStatus status, String reason) {

    public static EntitlementResult entitled(BillingStatus status, String reason) {
        return new EntitlementResult(true, status, reason);
    }

    public static EntitlementResult denied(BillingStatus status, String reason) {
        return new EntitlementResult(false, status, reason);
    }
}
