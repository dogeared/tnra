package com.afitnerd.tnra.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Category {

    private String best;
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
