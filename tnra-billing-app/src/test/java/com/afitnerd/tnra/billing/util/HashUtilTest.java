package com.afitnerd.tnra.billing.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashUtilTest {

    @Test
    void sha256Hex_knownVector() {
        // SHA-256("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            HashUtil.sha256Hex("abc"));
    }

    @Test
    void sha256Hex_isDeterministicAndDistinct() {
        assertEquals(HashUtil.sha256Hex("token"), HashUtil.sha256Hex("token"));
        assertNotEquals(HashUtil.sha256Hex("token"), HashUtil.sha256Hex("token2"));
    }

    @Test
    void hmacSha256Hex_knownVector() {
        // Well-known HMAC-SHA256 vector.
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
            HashUtil.hmacSha256Hex("The quick brown fox jumps over the lazy dog", "key"));
    }

    @Test
    void hmacSha256Hex_isDeterministicAndKeyed() {
        assertEquals(HashUtil.hmacSha256Hex("body", "s1"), HashUtil.hmacSha256Hex("body", "s1"));
        assertNotEquals(HashUtil.hmacSha256Hex("body", "s1"), HashUtil.hmacSha256Hex("body", "s2"));
    }

    @Test
    void constantTimeEquals() {
        assertTrue(HashUtil.constantTimeEquals("abc", "abc"));
        assertFalse(HashUtil.constantTimeEquals("abc", "abd"));
        assertFalse(HashUtil.constantTimeEquals(null, "abc"));
        assertFalse(HashUtil.constantTimeEquals("abc", null));
    }
}
