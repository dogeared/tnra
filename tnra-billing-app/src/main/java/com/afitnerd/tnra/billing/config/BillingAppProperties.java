package com.afitnerd.tnra.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Central billing app settings (distinct from {@link PaddleProperties}).
 * {@code admin-token} guards the provisioning/admin API; blank = admin API disabled (fail closed).
 * {@code trial-days} is the default group free-trial window applied at registration.
 */
@ConfigurationProperties(prefix = "billing")
public class BillingAppProperties {

    private String adminToken = "";
    private int trialDays = 60;
    /** Sender of member emails (reminders). Keep it on the Mailgun sending domain for DMARC alignment. */
    private String notifyFrom = "tnra-landing@tnra.app";
    /** Where a group's app lives; {@code {slug}} is replaced. Reminder links append {@code /profile?tab=billing}. */
    private String groupUrlPattern = "https://{slug}.tnra.app";
    /** How many days before the free period ends the reminder goes out. */
    private int reminderDaysBefore = 7;

    public String getAdminToken() {
        return adminToken;
    }

    public void setAdminToken(String adminToken) {
        this.adminToken = adminToken;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public String getNotifyFrom() {
        return notifyFrom;
    }

    public void setNotifyFrom(String notifyFrom) {
        this.notifyFrom = notifyFrom;
    }

    public String getGroupUrlPattern() {
        return groupUrlPattern;
    }

    public void setGroupUrlPattern(String groupUrlPattern) {
        this.groupUrlPattern = groupUrlPattern;
    }

    public int getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(int reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    /** Billing-tab deep link for a group, e.g. {@code https://the-og.tnra.app/profile?tab=billing}. */
    public String billingTabUrl(String groupSlug) {
        return groupUrlPattern.replace("{slug}", groupSlug) + "/profile?tab=billing";
    }
}
