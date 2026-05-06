package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostRendererTest {

    @Test
    void utf8ToAsciiConvertsSmartQuotes() {
        // \u2019 is right single quote (') → replaced with apostrophe
        String result = PostRenderer.utf8ToAscii("hello\u2019s");
        assertTrue(result.contains("'"));
        // \u201B is single high-reversed-9 quote (‛) → replaced with apostrophe
        String result2 = PostRenderer.utf8ToAscii("\u201Btest");
        assertTrue(result2.contains("'"));
        // \u201F is double high-reversed-9 quote (‟) → replaced with double quote
        String result3 = PostRenderer.utf8ToAscii("\u201Fquoted\u201D");
        assertTrue(result3.contains("\""));
    }

    @Test
    void utf8ToAsciiLeavesPlainTextAlone() {
        assertEquals("hello world", PostRenderer.utf8ToAscii("hello world"));
    }

    @Test
    void formatDateReturnsFormattedString() {
        Date date = new Date(1640995200000L); // 2022-01-01 00:00:00 UTC
        String formatted = PostRenderer.formatDate(date);
        assertNotNull(formatted);
        // Should contain date components (month/day/year)
        assertTrue(formatted.matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} [AP]M"));
    }
}
