package com.afitnerd.tnra.model.command;

public enum SubSection {
    WIDWYTK("wid"),
    KRYPTONITE("kry"),
    WHATANDWHEN("wha"),
    BEST("bes"),
    WORST("wor");

    private String value;

    SubSection(String value) {
        this.value = value;
    }

    public static SubSection fromValue(String value) {
        if (value.length() < 3) {
            throw new IllegalArgumentException("Bad value: " + value + ". Must be at least 3 characters");
        }
        String shorty = value.toLowerCase().substring(0, 3);
        switch (shorty) {
            case "wid":
                return SubSection.WIDWYTK;
            case "kry":
                return SubSection.KRYPTONITE;
            case "wha":
                return SubSection.WHATANDWHEN;
            case "bes":
                return SubSection.BEST;
            case "wor":
                return SubSection.WORST;
            default:
                throw new IllegalArgumentException("No SubSection named: " + value);
        }
    }
}
