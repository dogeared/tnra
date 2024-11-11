package com.afitnerd.tnra.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Intro {

    private String widwytk;
    private String kryptonite;
    private String whatAndWhen;

    public String getWidwytk() {
        return widwytk;
    }

    public void setWidwytk(String widwytk) {
        this.widwytk = widwytk;
    }

    public String getKryptonite() {
        return kryptonite;
    }

    public void setKryptonite(String kryptonite) {
        this.kryptonite = kryptonite;
    }

    public String getWhatAndWhen() {
        return whatAndWhen;
    }

    public void setWhatAndWhen(String whatAndWhen) {
        this.whatAndWhen = whatAndWhen;
    }
}
