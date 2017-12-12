package com.afitnerd.tnra.model.command;

import java.util.Arrays;
import java.util.List;

public enum Section {
    INTRO("int"),
    PERSONAL("per"),
    FAMILY("fam"),
    WORK("wor"),
    STATS("sta");

    private String value;

    Section(String value) {
        this.value = value;
    }

    public static Section fromValue(String value) {
        if (value.length() < 3) {
            throw new IllegalArgumentException("Bad value: " + value + ". Must be at least 3 characters");
        }
        String shorty = value.toLowerCase().substring(0, 3);
        switch (shorty) {
            case "int":
                return Section.INTRO;
            case "per":
                return Section.PERSONAL;
            case "fam":
                return Section.FAMILY;
            case "wor":
                return Section.WORK;
            case "sta":
                return Section.STATS;
            default:
                throw new IllegalArgumentException("No Section named: " + value);
        }
    }

    public static List<SubSection> validSubSectionsFor(Section section) {
        switch (section) {
            case INTRO:
                return Arrays.asList(SubSection.WIDWYTK, SubSection.KRYPTONITE, SubSection.WHATANDWHEN);
            case PERSONAL:
            case FAMILY:
            case WORK:
                return Arrays.asList(SubSection.BEST, SubSection.WORST);
            default:
                throw new IllegalArgumentException("No valid subsections for " + section + " Section");
        }
    }
}