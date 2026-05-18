package com.afitnerd.tnra.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretGeneratorTest {

    private final SecretGenerator generator = new SecretGenerator();

    @Test
    void defaultLength() {
        String secret = generator.generate();
        assertEquals(32, secret.length());
    }

    @Test
    void customLength() {
        assertEquals(16, generator.generate(16).length());
    }

    @Test
    void alphanumericOnly() {
        String secret = generator.generate();
        assertTrue(secret.matches("[A-Za-z0-9]+"),
            "Secret should be alphanumeric only, got: " + secret);
    }

    @Test
    void uniqueSecrets() {
        String a = generator.generate();
        String b = generator.generate();
        assertNotEquals(a, b);
    }
}
