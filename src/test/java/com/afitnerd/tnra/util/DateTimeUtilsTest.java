package com.afitnerd.tnra.util;

import com.afitnerd.tnra.vaadin.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void testFormatDateTime_withValidDate() {
        // Given: A specific date (January 15, 2025 at 2:30 PM)
        Date testDate = new Date(125, 0, 15, 14, 30, 0); // year offset from 1900, month 0-based
        
        // When
        String result = DateTimeUtils.formatDateTime(testDate);
        
        // Then: Should format correctly (exact format depends on system timezone)
        assertNotNull(result);
        assertTrue(result.contains("Jan"));
        assertTrue(result.contains("15"));
        assertTrue(result.contains("2025"));
        assertTrue(result.contains("at"));
        // Don't assert exact time as it may vary with timezone
        assertFalse(result.equals("Unknown"));
    }

    @Test
    void testFormatDateTime_withNullDate() {
        // When
        String result = DateTimeUtils.formatDateTime(null);
        
        // Then
        assertEquals("Unknown", result);
    }

    @Test
    void testFormatDateTime_withCurrentDate() {
        // Given
        Date now = new Date();
        
        // When
        String result = DateTimeUtils.formatDateTime(now);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertNotEquals("Unknown", result);
        assertTrue(result.contains("at")); // Should contain the " at " separator
    }
}