package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntitlementServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T12:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private GroupBillingRepository groupRepo;
    private BillingAccountRepository accountRepo;
    private EntitlementService service;

    @BeforeEach
    void setUp() {
        groupRepo = mock(GroupBillingRepository.class);
        accountRepo = mock(BillingAccountRepository.class);
        service = new EntitlementService(groupRepo, accountRepo, CLOCK);
    }

    private GroupBilling group(boolean exempt, LocalDateTime compUntil) {
        GroupBilling g = new GroupBilling("rome", "hash");
        g.setExempt(exempt);
        g.setCompUntil(compUntil);
        return g;
    }

    private void groupExists(GroupBilling g) {
        when(groupRepo.findByGroupSlug("rome")).thenReturn(Optional.of(g));
    }

    private BillingAccount account(BillingStatus status, boolean exempt, LocalDateTime compUntil) {
        BillingAccount a = new BillingAccount();
        a.setGroupSlug("rome");
        a.setEmail("m@x.com");
        a.setStatus(status);
        a.setExempt(exempt);
        a.setCompUntil(compUntil);
        return a;
    }

    private void accountExists(BillingAccount a) {
        when(accountRepo.findByGroupSlugAndEmail("rome", "m@x.com")).thenReturn(Optional.ofNullable(a));
    }

    @Test
    void deniesWhenGroupNotRegistered() {
        when(groupRepo.findByGroupSlug("rome")).thenReturn(Optional.empty());

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertFalse(r.entitled());
        assertEquals("GROUP_NOT_REGISTERED", r.reason());
    }

    @Test
    void groupExempt_entitled() {
        groupExists(group(true, null));
        accountExists(null);

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("GROUP_EXEMPT", r.reason());
    }

    @Test
    void memberExempt_entitled() {
        groupExists(group(false, null));
        accountExists(account(BillingStatus.SUSPENDED, true, null));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("MEMBER_EXEMPT", r.reason());
    }

    @Test
    void groupTrial_inFuture_entitled() {
        groupExists(group(false, NOW.plusDays(10)));
        accountExists(null);

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("GROUP_TRIAL", r.reason());
    }

    @Test
    void groupTrial_inPast_doesNotEntitle() {
        groupExists(group(false, NOW.minusDays(1)));
        accountExists(account(BillingStatus.PENDING_PAYMENT, false, null));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertFalse(r.entitled());
        assertEquals("NOT_ENTITLED", r.reason());
    }

    @Test
    void memberComp_inFuture_entitled() {
        groupExists(group(false, null));
        accountExists(account(BillingStatus.PENDING_PAYMENT, false, NOW.plusDays(5)));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("MEMBER_COMP", r.reason());
    }

    @Test
    void activeSubscription_entitled() {
        groupExists(group(false, null));
        accountExists(account(BillingStatus.ACTIVE, false, null));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("SUBSCRIPTION", r.reason());
        assertEquals(BillingStatus.ACTIVE, r.status());
    }

    @Test
    void gracePeriod_entitled() {
        groupExists(group(false, null));
        accountExists(account(BillingStatus.ON_GRACE_PERIOD, false, null));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertTrue(r.entitled());
        assertEquals("SUBSCRIPTION", r.reason());
    }

    @Test
    void suspended_notEntitled() {
        groupExists(group(false, null));
        accountExists(account(BillingStatus.SUSPENDED, false, null));

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertFalse(r.entitled());
        assertEquals(BillingStatus.SUSPENDED, r.status());
    }

    @Test
    void noAccount_pendingAndNotEntitled() {
        groupExists(group(false, null));
        accountExists(null);

        EntitlementResult r = service.isEntitled("rome", "m@x.com");

        assertFalse(r.entitled());
        assertEquals(BillingStatus.PENDING_PAYMENT, r.status());
        assertEquals("NOT_ENTITLED", r.reason());
    }
}
