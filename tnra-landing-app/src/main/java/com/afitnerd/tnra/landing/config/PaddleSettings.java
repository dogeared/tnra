package com.afitnerd.tnra.landing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paddle.js (client-side) settings for the {@code /pay} checkout page.
 *
 * <p>Driven by env vars (Spring relaxed binding):
 * <pre>
 *   PADDLE_CLIENT_TOKEN  -> tnra.paddle.client-token  (a client-side token: test_… / live_…)
 *   PADDLE_ENVIRONMENT   -> tnra.paddle.environment   (sandbox | production; default sandbox)
 * </pre>
 *
 * <p>The client-side token is distinct from the billing service's API key: it is meant to be embedded
 * in a public page and can only open checkouts, never read or change account data. The environment
 * must match the one the token was issued in — Paddle.js refuses a sandbox token in production and vice
 * versa. A blank token leaves the page in a "not configured" state rather than failing startup, so a
 * self-host / billing-off deploy of the landing site needs no Paddle config at all.
 */
@Component
public class PaddleSettings {

    public static final String SANDBOX = "sandbox";
    public static final String PRODUCTION = "production";

    private final String clientToken;
    private final String environment;

    public PaddleSettings(@Value("${tnra.paddle.client-token:}") String clientToken,
                          @Value("${tnra.paddle.environment:sandbox}") String environment) {
        String env = environment == null ? "" : environment.trim().toLowerCase();
        if (!SANDBOX.equals(env) && !PRODUCTION.equals(env)) {
            throw new IllegalStateException(
                "tnra.paddle.environment must be '" + SANDBOX + "' or '" + PRODUCTION + "' (got '"
                + environment + "'). Set PADDLE_ENVIRONMENT.");
        }
        this.clientToken = clientToken == null ? "" : clientToken.trim();
        this.environment = env;
    }

    /** True when a client-side token is present, i.e. the {@code /pay} page can open a checkout. */
    public boolean isConfigured() {
        return !clientToken.isEmpty();
    }

    public String getClientToken() {
        return clientToken;
    }

    public String getEnvironment() {
        return environment;
    }

    public boolean isSandbox() {
        return SANDBOX.equals(environment);
    }
}
