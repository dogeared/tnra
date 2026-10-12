package com.afitnerd.tnra.landing.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaddleSettingsTest {

    @Test
    void sandboxToken_isConfigured() {
        PaddleSettings s = new PaddleSettings(" test_abc ", "sandbox");
        assertTrue(s.isConfigured());
        assertTrue(s.isSandbox());
        assertEquals("test_abc", s.getClientToken());
        assertEquals("sandbox", s.getEnvironment());
    }

    @Test
    void productionEnvironment_isNormalized() {
        PaddleSettings s = new PaddleSettings("live_abc", " Production ");
        assertFalse(s.isSandbox());
        assertEquals("production", s.getEnvironment());
    }

    @Test
    void blankToken_isNotConfigured_butStillBoots() {
        assertFalse(new PaddleSettings("", "sandbox").isConfigured());
        assertFalse(new PaddleSettings("   ", "sandbox").isConfigured());
        assertFalse(new PaddleSettings(null, "sandbox").isConfigured());
    }

    @Test
    void unknownEnvironment_failsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new PaddleSettings("test_abc", "staging"));
        assertTrue(ex.getMessage().contains("PADDLE_ENVIRONMENT"));
        assertThrows(IllegalStateException.class, () -> new PaddleSettings("test_abc", null));
    }
}
