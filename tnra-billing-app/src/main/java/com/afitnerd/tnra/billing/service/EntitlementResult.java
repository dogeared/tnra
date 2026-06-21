package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingStatus;

/**
 * Outcome of an entitlement check. {@code status} is the member's current billing status
 * (PENDING_PAYMENT when there's no account yet); {@code reason} names which rule decided it;
 * {@code payerEmail} is who pays for this member (null/blank for self-pay, set when the membership is
 * a gift from that person) so callers can show "gifted by …".
 */
public record EntitlementResult(boolean entitled, BillingStatus status, String reason, String payerEmail) {

    public static EntitlementResult entitled(BillingStatus status, String reason) {
        return new EntitlementResult(true, status, reason, null);
    }

    public static EntitlementResult entitled(BillingStatus status, String reason, String payerEmail) {
        return new EntitlementResult(true, status, reason, payerEmail);
    }

    public static EntitlementResult denied(BillingStatus status, String reason) {
        return new EntitlementResult(false, status, reason, null);
    }
}
