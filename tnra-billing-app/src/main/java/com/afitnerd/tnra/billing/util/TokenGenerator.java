package com.afitnerd.tnra.billing.util;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates the per-group API bearer tokens (only the SHA-256 hash is stored). */
public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {}

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
