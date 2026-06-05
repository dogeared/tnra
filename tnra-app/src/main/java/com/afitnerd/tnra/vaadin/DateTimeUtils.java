package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;

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

    public static String formatDateTime(Date date) {
        return formatDateTime(date, "MMM dd, yyyy 'at' h:mm a");
    }

    /**
     * Formats a Date object to a user-friendly string in the specified timezone.
     * 
     * @param date the Date to format, can be null
     * @param zoneId the timezone to format the date in
     * @return formatted date string in format "MMM dd, yyyy 'at' h:mm a" in specified timezone,
     *         or "Unknown" if date is null
     */
    public static String formatDateTime(Date date, String formatPattern) {
        if (date == null) {
            return "Unknown";
        }
        if (formatPattern == null) {
            formatPattern = "MMM dd, yyyy 'at' h:mm a";
        }

        ZoneId displayZone = ZoneId.systemDefault();
        if (
            UI.getCurrent() != null &&
            UI.getCurrent().getSession() != null &&
            UI.getCurrent().getSession().getAttribute(ExtendedClientDetails.class) != null
        ) {
            displayZone =
                ZoneId.of(UI.getCurrent().getSession().getAttribute(ExtendedClientDetails.class).getTimeZoneId());
        }
        LocalDateTime localDateTime = date.toInstant().atZone(displayZone).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        return localDateTime.format(formatter);
    }
}