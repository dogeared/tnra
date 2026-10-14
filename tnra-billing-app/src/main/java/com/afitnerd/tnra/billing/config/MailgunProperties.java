package com.afitnerd.tnra.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mailgun credentials for member emails (the "free period ending" reminder). Same env as the landing
 * site: {@code MAILGUN_KEY_PRIVATE} and {@code MAILGUN_URL} (the full messages endpoint of an active
 * sending domain). Blank ⇒ the mailer reports itself unconfigured and reminders are skipped with a log.
 */
@ConfigurationProperties(prefix = "mailgun")
public class MailgunProperties {

    private String keyPrivate = "";
    private String url = "";

    public String getKeyPrivate() {
        return keyPrivate;
    }

    public void setKeyPrivate(String keyPrivate) {
        this.keyPrivate = keyPrivate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isConfigured() {
        return !keyPrivate.isBlank() && !url.isBlank();
    }
}
