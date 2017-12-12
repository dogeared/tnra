package com.afitnerd.tnra.model.command;

import java.util.Arrays;
import java.util.List;

public enum Stat {

    EXERCISE("exe"),
    GTG("gtg"),
    MEDITATE("med"),
    MEETINGS("mee"),
    PRAY("pra"),
    READ("rea"),
    SPONSOR("spo");

    private String value;

    Stat(String value) {
        this.value = value;
    }

    public static Stat fromValue(String value) {
        if (value.length() < 3) {
            throw new IllegalArgumentException("Bad action: " + value + ". Must be at least 3 characters");
        }
        String shorty = value.toLowerCase().substring(0, 3);
        switch (shorty) {
            case "exe":
                return Stat.EXERCISE;
            case "gtg":
                return Stat.GTG;
            case "med":
                return Stat.MEDITATE;
            case "mee":
                return Stat.MEETINGS;
            case "pra":
                return Stat.PRAY;
            case "rea":
                return Stat.READ;
            case "spo":
                return Stat.SPONSOR;
            default:
                throw new IllegalArgumentException("No Stat named: " + value);
        }
    }

    public static List<Stat> allStats() {
        return Arrays.asList(Stat.EXERCISE, Stat.GTG, Stat.MEDITATE, Stat.MEETINGS, Stat.PRAY, Stat.READ, Stat.SPONSOR);
    }
}
