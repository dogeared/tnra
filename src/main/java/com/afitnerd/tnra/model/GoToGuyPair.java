package com.afitnerd.tnra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class GoToGuyPair {

    @JsonView(JsonViews.Sparse.class)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @JsonView(JsonViews.Sparse.class)
    @OneToOne
    private User caller;

    @JsonView(JsonViews.Sparse.class)
    @OneToOne
    private User callee;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties("goToGuyPairs")
    private GoToGuySet goToGuySet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCaller() {
        return caller;
    }

    public void setCaller(User caller) {
        this.caller = caller;
    }

    public User getCallee() {
        return callee;
    }

    public void setCallee(User callee) {
        this.callee = callee;
    }

    public GoToGuySet getGoToGuySet() {
        return goToGuySet;
    }

    public void setGoToGuySet(GoToGuySet goToGuySet) {
        this.goToGuySet = goToGuySet;
    }
}
