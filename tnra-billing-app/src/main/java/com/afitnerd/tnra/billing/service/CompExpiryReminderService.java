package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * "Your free period ends soon" reminder. A member covered only by a time-boxed waiver — the group's
 * trial ({@code group_billing.comp_until}) or their own ({@code billing_account.comp_until}) — has no
 * card on file, so when it lapses they are simply denied at the next login. This sweep emails them
 * {@code billing.reminder-days-before} days ahead with a link to their Billing tab.
 *
 * <p>Scope: members the billing service knows about (rows in {@code billing_account}); a member who has
 * never touched billing has no row and no email address here. Skipped: exempt groups/members, members
 * with a live subscription (ACTIVE / ON_GRACE_PERIOD — they already pay), and ends beyond the window
 * (a member waiver outlasting the group trial). One email per distinct coverage-end date
 * ({@code comp_reminder_for}), so a daily run never repeats itself and a later waiver gets its own.
 */
@Service
public class CompExpiryReminderService {

    private static final Logger log = LoggerFactory.getLogger(CompExpiryReminderService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

    private final BillingAccountRepository accounts;
    private final GroupBillingRepository groups;
    private final MailgunMailer mailer;
    private final BillingAppProperties props;
    private final Clock clock;

    public CompExpiryReminderService(BillingAccountRepository accounts, GroupBillingRepository groups,
                                     MailgunMailer mailer, BillingAppProperties props, Clock clock) {
        this.accounts = accounts;
        this.groups = groups;
        this.mailer = mailer;
        this.props = props;
        this.clock = clock;
    }

    /** Daily at 09:00 server time by default ({@code billing.reminder-cron}). */
    @Scheduled(cron = "${billing.reminder-cron:0 0 9 * * *}")
    public void scheduledSweep() {
        try {
            int sent = sweep();
            log.info("Comp-expiry reminder sweep sent {} email(s)", sent);
        } catch (RuntimeException e) {
            log.error("Comp-expiry reminder sweep failed: {}", e.getMessage(), e);
        }
    }

    /** @return number of reminders sent. */
    public int sweep() {
        if (!mailer.isConfigured()) {
            log.info("Comp-expiry reminders skipped: Mailgun not configured");
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime horizon = now.plusDays(props.getReminderDaysBefore());

        // Candidates: members whose own waiver ends in the window, plus every known member of a group
        // whose trial ends in the window. Deduped by id; the per-account checks below decide.
        Map<Long, BillingAccount> candidates = new LinkedHashMap<>();
        accounts.findByCompUntilBetween(now, horizon).forEach(a -> candidates.put(a.getId(), a));
        for (GroupBilling g : groups.findByCompUntilBetween(now, horizon)) {
            accounts.findByGroupSlug(g.getGroupSlug()).forEach(a -> candidates.putIfAbsent(a.getId(), a));
        }

        int sent = 0;
        for (BillingAccount account : candidates.values()) {
            GroupBilling group = groups.findByGroupSlug(account.getGroupSlug()).orElse(null);
            LocalDateTime end = coverageEnd(group, account, now);
            if (end == null || end.isAfter(horizon) || !needsReminder(group, account, end)) {
                continue;
            }
            try {
                mailer.send(props.getNotifyFrom(), account.getEmail(), subject(end), body(account, end));
            } catch (RuntimeException e) {
                log.warn("Reminder to {} ({}) failed: {} — will retry on the next sweep",
                    account.getEmail(), account.getGroupSlug(), e.getMessage());
                continue;
            }
            account.setCompReminderFor(end);
            accounts.save(account);
            sent++;
        }
        return sent;
    }

    /** The latest still-future waiver covering this member (group trial or own comp), or null. */
    static LocalDateTime coverageEnd(GroupBilling group, BillingAccount account, LocalDateTime now) {
        LocalDateTime groupEnd = group == null ? null : future(group.getCompUntil(), now);
        LocalDateTime memberEnd = future(account.getCompUntil(), now);
        if (groupEnd == null) {
            return memberEnd;
        }
        return memberEnd != null && memberEnd.isAfter(groupEnd) ? memberEnd : groupEnd;
    }

    private static LocalDateTime future(LocalDateTime when, LocalDateTime now) {
        return when != null && when.isAfter(now) ? when : null;
    }

    static boolean needsReminder(GroupBilling group, BillingAccount account, LocalDateTime end) {
        if (group == null || Boolean.TRUE.equals(group.getExempt()) || Boolean.TRUE.equals(account.getExempt())) {
            return false;
        }
        if (account.getStatus() == BillingStatus.ACTIVE || account.getStatus() == BillingStatus.ON_GRACE_PERIOD) {
            return false; // already paying — the waiver ending changes nothing for them
        }
        return !end.equals(account.getCompReminderFor());
    }

    static String subject(LocalDateTime end) {
        return "Your TNRA free period ends " + DATE.format(end.toLocalDate());
    }

    String body(BillingAccount account, LocalDateTime end) {
        String link = props.billingTabUrl(account.getGroupSlug());
        return "<p>Hi,</p>"
            + "<p>Your free period in your TNRA group ends on <strong>" + DATE.format(end.toLocalDate())
            + "</strong>. After that you'll need an active membership to keep posting and reading your group.</p>"
            + "<p>To add a card before then, sign in and open your Billing tab: "
            + "<a href=\"" + link + "\">" + link + "</a></p>"
            + "<p>If someone else in your group is covering your membership, you can ignore this.</p>"
            + "<p>— TNRA</p>";
    }
}
