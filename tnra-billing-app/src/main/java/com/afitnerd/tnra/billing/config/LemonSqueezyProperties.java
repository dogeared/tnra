package com.afitnerd.tnra.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lemon Squeezy (Merchant of Record) configuration. All values come from env in production
 * (LS_API_KEY, LS_STORE_ID, LS_WEBHOOK_SECRET, LS_VARIANT_MONTHLY, LS_VARIANT_YEARLY) and are
 * blank by default so a misconfigured deploy fails loudly rather than charging the wrong store.
 */
@ConfigurationProperties(prefix = "lemonsqueezy")
public class LemonSqueezyProperties {

    private String apiKey = "";
    private String storeId = "";
    private String webhookSecret = "";
    private Variant variant = new Variant();

    public static class Variant {
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

    /** Resolve a variant name ("monthly" | "yearly") to its Lemon Squeezy variant id. */
    public String variantId(String name) {
        if ("monthly".equalsIgnoreCase(name)) {
            return variant.getMonthly();
        }
        if ("yearly".equalsIgnoreCase(name)) {
            return variant.getYearly();
        }
        throw new IllegalArgumentException("Unknown billing variant: " + name);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }
}
