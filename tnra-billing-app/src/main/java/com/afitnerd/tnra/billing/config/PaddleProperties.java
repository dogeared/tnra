package com.afitnerd.tnra.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paddle (Merchant of Record) configuration. All secrets come from env in production
 * (PADDLE_API_KEY, PADDLE_WEBHOOK_SECRET, PADDLE_PRICE_MONTHLY, PADDLE_PRICE_YEARLY) and are blank by
 * default so a misconfigured deploy fails loudly rather than charging the wrong account.
 *
 * <p>{@code api-base} defaults to Paddle's <b>sandbox</b>; point it at {@code https://api.paddle.com}
 * for live. {@code webhook-secret} is the notification destination's secret key (prefixed
 * {@code pdl_ntfset_}). {@code price.monthly} / {@code price.yearly} are Paddle Price ids (the Paddle
 * equivalent of Lemon Squeezy variants).
 */
@ConfigurationProperties(prefix = "paddle")
public class PaddleProperties {

    private String apiKey = "";
    private String apiBase = "https://sandbox-api.paddle.com";
    private String webhookSecret = "";
    private Price price = new Price();

    public static class Price {
        private String monthly = "";
        private String yearly = "";

        public String getMonthly() {
            return monthly;
        }

        public void setMonthly(String monthly) {
            this.monthly = monthly;
        }

        public String getYearly() {
            return yearly;
        }

        public void setYearly(String yearly) {
            this.yearly = yearly;
        }
    }

    /** Resolve a plan name ("monthly" | "yearly") to its Paddle Price id. */
    public String priceId(String name) {
        if ("monthly".equalsIgnoreCase(name)) {
            return price.getMonthly();
        }
        if ("yearly".equalsIgnoreCase(name)) {
            return price.getYearly();
        }
        throw new IllegalArgumentException("Unknown billing variant: " + name);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiBase() {
        return apiBase;
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }
}
