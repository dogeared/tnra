package com.afitnerd.tnra.model;

import javax.persistence.Embeddable;

@Embeddable
public class Stats {

    private Integer exercise;
    private Integer gtg;
    private Integer meditate;
    private Integer meetings;
    private Integer pray;
    private Integer read;
    private Integer sponsor;

    public Integer getExercise() {
        return exercise;
    }

    public void setExercise(Integer exercise) {
        this.exercise = exercise;
    }

    public Integer getGtg() {
        return gtg;
    }

    public void setGtg(Integer gtg) {
        this.gtg = gtg;
    }

    public Integer getMeditate() {
        return meditate;
    }

    public void setMeditate(Integer meditate) {
        this.meditate = meditate;
    }

    public Integer getMeetings() {
        return meetings;
    }

    public void setMeetings(Integer meetings) {
        this.meetings = meetings;
    }

    public Integer getPray() {
        return pray;
    }

    public void setPray(Integer pray) {
        this.pray = pray;
    }

    public Integer getRead() {
        return read;
    }

    public void setRead(Integer read) {
        this.read = read;
    }

    public Integer getSponsor() {
        return sponsor;
    }

    public void setSponsor(Integer sponsor) {
        this.sponsor = sponsor;
    }
}
