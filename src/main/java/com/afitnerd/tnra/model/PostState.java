package com.afitnerd.tnra.model;

public enum PostState {
    IN_PROGRESS("in_progress"), COMPLETE("complete");

    private String value;

    private PostState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PostState fromValue(String value) {
        switch(value) {
            case "in_progress":
                return PostState.IN_PROGRESS;
            case "complete":
                return PostState.COMPLETE;
            default:
                throw new IllegalArgumentException("Bad value passed in: " + value);
        }
    }
}
