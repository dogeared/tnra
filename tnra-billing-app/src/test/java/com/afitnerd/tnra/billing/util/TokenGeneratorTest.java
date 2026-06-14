package com.afitnerd.tnra.billing.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TokenGeneratorTest {

    @Test
    void newToken_isNonBlankUrlSafeAndDistinct() {
        String a = TokenGenerator.newToken();
        String b = TokenGenerator.newToken();

        assertFalse(a.isBlank());
        assertNotEquals(a, b);
        assertEquals(43, a.length()); // 32 bytes, base64url no padding
        assertFalse(a.contains("+") || a.contains("/") || a.contains("="));
    }
}
