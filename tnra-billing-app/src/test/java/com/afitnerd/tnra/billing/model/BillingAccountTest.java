package com.afitnerd.tnra.billing.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingAccountTest {

    private BillingAccount account(String email, String payerEmail) {
        BillingAccount a = new BillingAccount();
        a.setEmail(email);
        a.setPayerEmail(payerEmail);
        return a;
    }

    @Test
    void isGift_falseForSelfPay() {
        assertFalse(account("m@x.com", null).isGift());
    }

    @Test
    void isGift_falseWhenPayerIsBeneficiary_caseInsensitive() {
        assertFalse(account("m@x.com", "M@X.com").isGift());
    }

    @Test
    void isGift_trueWhenPayerDiffers() {
        assertTrue(account("m@x.com", "admin@x.com").isGift());
    }

    @Test
    void onCreate_setsTimestampsAndDefaults() {
        BillingAccount a = new BillingAccount();
        a.setStatus(null);
        a.setExempt(null);

        a.onCreate();

        assertNotNull(a.getCreatedAt());
        assertNotNull(a.getUpdatedAt());
        assertEquals(BillingStatus.PENDING_PAYMENT, a.getStatus());
        assertFalse(a.getExempt());
    }

    @Test
    void onCreate_preservesExistingCreatedAt() {
        BillingAccount a = new BillingAccount();
        java.time.LocalDateTime earlier = java.time.LocalDateTime.now().minusDays(3);
        a.setCreatedAt(earlier);

        a.onCreate();

        assertEquals(earlier, a.getCreatedAt());
    }

    @Test
    void onUpdate_refreshesUpdatedAt() {
        BillingAccount a = new BillingAccount();
        java.time.LocalDateTime stale = java.time.LocalDateTime.now().minusDays(3);
        a.setUpdatedAt(stale);

        a.onUpdate();

        assertNotNull(a.getUpdatedAt());
        assertTrue(a.getUpdatedAt().isAfter(stale));
    }
}
