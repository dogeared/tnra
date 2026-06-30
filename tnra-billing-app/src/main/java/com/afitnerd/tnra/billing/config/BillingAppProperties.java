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
}
