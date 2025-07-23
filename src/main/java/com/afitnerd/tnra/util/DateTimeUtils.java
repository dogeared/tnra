package com.afitnerd.tnra.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility class for date and time formatting operations.
 * Provides consistent date/time formatting across the application.
 */
public final class DateTimeUtils {

    private DateTimeUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Formats a Date object to a user-friendly string in the user's local timezone.
     * 
     * @param date the Date to format, can be null
     * @return formatted date string in format "MMM dd, yyyy 'at' h:mm a" in user's timezone,
     *         or "Unknown" if date is null
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return "Unknown";
        }
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a");
        return localDateTime.format(formatter);
    }
}