package com.afitnerd.tnra.model.command;

import java.util.Arrays;
import java.util.List;

public enum Action {
    START("sta"),
    FINISH("fin"),
    SHOW("sho"),
    EMAIL("ema"),
    UPDATE("upd"),
    REPLACE("rep"),
    APPEND("app"),
    HELP("hel");

    private String value;

    Action(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Action fromValue(String value) {
        if (value.length() < 3) {
            throw new IllegalArgumentException("Bad action: "+ value + ". Must be at least 3 characters");
        }
        String shorty = value.toLowerCase().substring(0, 3);
        switch (shorty) {
            case "sta":
                return Action.START;
            case "fin":
                return Action.FINISH;
            case "sho":
                return Action.SHOW;
            case "ema":
                return Action.EMAIL;
            case "upd":
                return Action.UPDATE;
            case "rep":
                return Action.REPLACE;
            case "app":
                return Action.APPEND;
            case "hel":
                return Action.HELP;
            default:
                throw new IllegalArgumentException("No Action named: " + value);
        }
    }

    public static List<Section> validSectionsFor(Action action) {
        if (UPDATE.equals(action) || REPLACE.equals(action) || APPEND.equals(action)) {
            return Arrays.asList(Section.INTRO, Section.PERSONAL, Section.FAMILY, Section.WORK, Section.STATS);
        }
        return null;
    }

    public static List<Action> standaloneActions() {
        return Arrays.asList(Action.START, Action.FINISH, Action.SHOW, Action.EMAIL, Action.HELP);
    }
}