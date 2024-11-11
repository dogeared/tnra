package com.afitnerd.tnra.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Date;
import java.util.List;

@Entity
public class GoToGuySet {

    @JsonView(JsonViews.Sparse.class)
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @JsonView(JsonViews.Sparse.class)
    private Date startDate;

    @JsonView(JsonViews.Sparse.class)
    @OneToMany(mappedBy = "goToGuySet", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GoToGuyPair> goToGuyPairs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public List<GoToGuyPair> getGoToGuyPairs() {
        return goToGuyPairs;
    }

    public void setGoToGuyPairs(List<GoToGuyPair> goToGuyPairs) {
        this.goToGuyPairs = goToGuyPairs;
    }
}
