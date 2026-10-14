package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompExpiryReminderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 9, 0);

    private BillingAccountRepository accounts;
    private GroupBillingRepository groups;
    private MailgunMailer mailer;
    private BillingAppProperties props;
    private CompExpiryReminderService service;

    @BeforeEach
    void setUp() {
        accounts = mock(BillingAccountRepository.class);
        groups = mock(GroupBillingRepository.class);
        mailer = mock(MailgunMailer.class);
        when(mailer.isConfigured()).thenReturn(true);
        props = new BillingAppProperties();
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new CompExpiryReminderService(accounts, groups, mailer, props, clock);
        when(accounts.findByCompUntilBetween(any(), any())).thenReturn(List.of());
        when(groups.findByCompUntilBetween(any(), any())).thenReturn(List.of());
        when(accounts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private GroupBilling group(String slug, LocalDateTime compUntil, boolean exempt) {
        GroupBilling g = new GroupBilling();
        g.setGroupSlug(slug);
        g.setCompUntil(compUntil);
        g.setExempt(exempt);
        when(groups.findByGroupSlug(slug)).thenReturn(Optional.of(g));
        return g;
    }

    private BillingAccount account(long id, String slug, String email, BillingStatus status, LocalDateTime compUntil) {
        BillingAccount a = new BillingAccount();
        a.setId(id);
        a.setGroupSlug(slug);
        a.setEmail(email);
        a.setStatus(status);
        a.setCompUntil(compUntil);
        return a;
    }

    @Test
    void memberWaiverEndingInsideWindow_getsOneEmail_withBillingLink_andIsMarked() {
        group("rome", null, false);
        BillingAccount a = account(1, "rome", "m@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(5));
        when(accounts.findByCompUntilBetween(NOW, NOW.plusDays(7))).thenReturn(List.of(a));

        assertEquals(1, service.sweep());

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("tnra-landing@tnra.app"), eq("m@x.com"),
            eq("Your TNRA free period ends " + CompExpiryReminderService.subject(NOW.plusDays(5)).substring(27)),
            html.capture());
        assertTrue(html.getValue().contains("https://rome.tnra.app/profile?tab=billing"));
        assertEquals(NOW.plusDays(5), a.getCompReminderFor());
        verify(accounts).save(a);

        // Second sweep for the same end date: nothing more.
        assertEquals(0, service.sweep());
        verify(mailer, times(1)).send(any(), any(), any(), any());
    }

    @Test
    void groupTrialEndingInsideWindow_remindsEveryKnownMemberWithoutASubscription() {
        GroupBilling g = group("rome", NOW.plusDays(3), false);
        when(groups.findByCompUntilBetween(NOW, NOW.plusDays(7))).thenReturn(List.of(g));
        BillingAccount pending = account(1, "rome", "p@x.com", BillingStatus.PENDING_PAYMENT, null);
        BillingAccount paying = account(2, "rome", "a@x.com", BillingStatus.ACTIVE, null);
        BillingAccount dunning = account(3, "rome", "g@x.com", BillingStatus.ON_GRACE_PERIOD, null);
        BillingAccount suspended = account(4, "rome", "s@x.com", BillingStatus.SUSPENDED, null);
        when(accounts.findByGroupSlug("rome")).thenReturn(List.of(pending, paying, dunning, suspended));

        assertEquals(2, service.sweep());

        verify(mailer).send(any(), eq("p@x.com"), any(), any());
        verify(mailer).send(any(), eq("s@x.com"), any(), any());
        verify(mailer, never()).send(any(), eq("a@x.com"), any(), any());
        verify(mailer, never()).send(any(), eq("g@x.com"), any(), any());
        assertEquals(NOW.plusDays(3), pending.getCompReminderFor());
    }

    @Test
    void exemptGroupOrMember_andEndsBeyondTheWindow_areSkipped() {
        group("free", NOW.plusDays(2), true);
        BillingAccount inExemptGroup = account(1, "free", "e@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(2));
        group("rome", null, false);
        BillingAccount exemptMember = account(2, "rome", "x@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(2));
        exemptMember.setExempt(true);
        // Group trial ends in 4 days but this member's own waiver runs 30 days — nothing to warn about yet.
        GroupBilling soon = group("athens", NOW.plusDays(4), false);
        BillingAccount longWaiver = account(3, "athens", "l@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(30));
        when(accounts.findByCompUntilBetween(any(), any())).thenReturn(List.of(inExemptGroup, exemptMember));
        when(groups.findByCompUntilBetween(any(), any())).thenReturn(List.of(soon));
        when(accounts.findByGroupSlug("athens")).thenReturn(List.of(longWaiver));

        assertEquals(0, service.sweep());
        verify(mailer, never()).send(any(), any(), any(), any());
        verify(accounts, never()).save(any());
    }

    @Test
    void aLaterWaiver_getsItsOwnReminder_afterTheFirstOneWasSent() {
        group("rome", null, false);
        BillingAccount a = account(1, "rome", "m@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(6));
        a.setCompReminderFor(NOW.minusDays(20)); // reminded about a previous, earlier end date
        when(accounts.findByCompUntilBetween(any(), any())).thenReturn(List.of(a));

        assertEquals(1, service.sweep());
        assertEquals(NOW.plusDays(6), a.getCompReminderFor());
    }

    @Test
    void mailFailure_isLogged_notMarked_soTheNextSweepRetries() {
        group("rome", null, false);
        BillingAccount a = account(1, "rome", "m@x.com", BillingStatus.PENDING_PAYMENT, NOW.plusDays(1));
        when(accounts.findByCompUntilBetween(any(), any())).thenReturn(List.of(a));
        doThrow(new IllegalStateException("mailgun 403")).when(mailer).send(any(), any(), any(), any());

        assertEquals(0, service.sweep());
        assertNull(a.getCompReminderFor());
        verify(accounts, never()).save(any());
    }

    @Test
    void unconfiguredMailer_skipsTheSweepEntirely() {
        when(mailer.isConfigured()).thenReturn(false);
        assertEquals(0, service.sweep());
        verify(accounts, never()).findByCompUntilBetween(any(), any());
    }

    @Test
    void scheduledSweep_swallowsFailures() {
        when(accounts.findByCompUntilBetween(any(), any())).thenThrow(new IllegalStateException("db down"));
        service.scheduledSweep(); // must not propagate — the scheduler thread would otherwise log and stop
    }

    @Test
    void coverageEnd_isTheLaterOfGroupAndMemberWaivers_ignoringThePast() {
        GroupBilling g = new GroupBilling();
        g.setCompUntil(NOW.plusDays(3));
        BillingAccount a = new BillingAccount();
        assertEquals(NOW.plusDays(3), CompExpiryReminderService.coverageEnd(g, a, NOW));
        a.setCompUntil(NOW.plusDays(10));
        assertEquals(NOW.plusDays(10), CompExpiryReminderService.coverageEnd(g, a, NOW));
        a.setCompUntil(NOW.minusDays(1));
        assertEquals(NOW.plusDays(3), CompExpiryReminderService.coverageEnd(g, a, NOW));
        g.setCompUntil(NOW.minusDays(1));
        assertNull(CompExpiryReminderService.coverageEnd(g, a, NOW));
        assertNull(CompExpiryReminderService.coverageEnd(null, a, NOW));
    }

    @Test
    void needsReminder_requiresARegisteredNonExemptGroup() {
        BillingAccount a = new BillingAccount();
        a.setStatus(BillingStatus.PENDING_PAYMENT);
        assertFalse(CompExpiryReminderService.needsReminder(null, a, NOW));
    }
}
