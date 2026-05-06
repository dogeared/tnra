package com.afitnerd.tnra.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmTest {

    private byte[] randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    @Test
    void encryptDecryptRoundTrip() {
        byte[] key = randomKey();
        String original = "Hello, TNRA encryption!";
        byte[] encrypted = AesGcm.encrypt(original.getBytes(StandardCharsets.UTF_8), key);
        byte[] decrypted = AesGcm.decrypt(encrypted, key);
        assertEquals(original, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        byte[] key = randomKey();
        byte[] plaintext = "same plaintext".getBytes(StandardCharsets.UTF_8);
        byte[] ct1 = AesGcm.encrypt(plaintext, key);
        byte[] ct2 = AesGcm.encrypt(plaintext, key);
        assertFalse(java.util.Arrays.equals(ct1, ct2), "random IV should produce different ciphertexts");
    }

    @Test
    void encryptedOutputIsLongerThanPlaintext() {
        byte[] key = randomKey();
        byte[] plaintext = "short".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = AesGcm.encrypt(plaintext, key);
        // IV (12) + plaintext length + GCM tag (16) = plaintext.length + 28
        assertEquals(plaintext.length + 28, encrypted.length);
    }

    @Test
    void decryptWithWrongKeyThrows() {
        byte[] key = randomKey();
        byte[] wrongKey = randomKey();
        byte[] encrypted = AesGcm.encrypt("secret".getBytes(StandardCharsets.UTF_8), key);
        assertThrows(IllegalStateException.class, () -> AesGcm.decrypt(encrypted, wrongKey));
    }

    @Test
    void encryptEmptyString() {
        byte[] key = randomKey();
        byte[] plaintext = "".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = AesGcm.encrypt(plaintext, key);
        byte[] decrypted = AesGcm.decrypt(encrypted, key);
        assertEquals("", new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void encryptLongString() {
        byte[] key = randomKey();
        String longText = "x".repeat(10000);
        byte[] encrypted = AesGcm.encrypt(longText.getBytes(StandardCharsets.UTF_8), key);
        byte[] decrypted = AesGcm.decrypt(encrypted, key);
        assertEquals(longText, new String(decrypted, StandardCharsets.UTF_8));
    }
}
