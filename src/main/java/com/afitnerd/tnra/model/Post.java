package com.afitnerd.tnra.model;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Date start;
    private Date finish;
    private PostState state;

    @Embedded
    private Intro intro;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "personal_best")),
        @AttributeOverride(name = "worst", column = @Column(name = "personal_worst"))
    })
    private Category personal;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "family_best")),
        @AttributeOverride(name = "worst", column = @Column(name = "family_worst"))
    })
    private Category family;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "work_best")),
        @AttributeOverride(name = "worst", column = @Column(name = "work_worst"))
    })
    private Category work;

    @Embedded
    private Stats stats;

    @ManyToOne(optional = false)
    private User user;

    public Post() {
        setState(PostState.IN_PROGRESS);
        setStart(new Date());
    }

    public Post(User user) {
        this();
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getFinish() {
        return finish;
    }

    public void setFinish(Date finish) {
        this.finish = finish;
    }

    public PostState getState() {
        return state;
    }

    public void setState(PostState state) {
        this.state = state;
    }

    public Intro getIntro() {
        if (intro == null) { intro = new Intro(); }
        return intro;
    }

    public void setIntro(Intro intro) {
        this.intro = intro;
    }

    public Category getPersonal() {
        if (personal == null) { personal = new Category(); }
        return personal;
    }

    public void setPersonal(Category personal) {
        this.personal = personal;
    }

    public Category getFamily() {
        if (family == null) { family = new Category(); }
        return family;
    }

    public void setFamily(Category family) {
        this.family = family;
    }

    public Category getWork() {
        if (work == null) { work = new Category(); }
        return work;
    }

    public void setWork(Category work) {
        this.work = work;
    }

    public Stats getStats() {
        if (stats == null) { stats = new Stats(); }
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
