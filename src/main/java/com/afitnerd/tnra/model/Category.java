package com.afitnerd.tnra.model;

import com.afitnerd.tnra.model.converter.EncryptedStringConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

@Embeddable
public class Category {

    @Convert(converter = EncryptedStringConverter.class)
    private String best;

    @Convert(converter = EncryptedStringConverter.class)
    private String worst;

    public String getBest() {
        return best;
    }

    public void setBest(String best) {
        this.best = best;
    }

    public String getWorst() {
        return worst;
    }

    public void setWorst(String worst) {
        this.worst = worst;
    }
}
