package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public interface PostRenderer {

    String render(Post post);

    static String formatDate(Date date) {
        TimeZone est = TimeZone.getTimeZone("US/Eastern");
        GregorianCalendar cal = new GregorianCalendar(est);
        cal.setTime(date);
        ZonedDateTime zdt = cal.toZonedDateTime();
        LocalDateTime dateTime = zdt.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
        return dateTime.format(formatter);
    }
}
