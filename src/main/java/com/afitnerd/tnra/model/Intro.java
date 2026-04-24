package com.afitnerd.tnra.model;

import com.afitnerd.tnra.model.converter.EncryptedStringConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

@Embeddable
public class Intro {

    @Convert(converter = EncryptedStringConverter.class)
    private String widwytk;

    @Convert(converter = EncryptedStringConverter.class)
    private String kryptonite;

    @Convert(converter = EncryptedStringConverter.class)
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
