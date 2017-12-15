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

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (intro == null) {
            sb.append("Intro not set\n\n");
        } else {
            sb.append("Intro:\n\n");

            sb.append("WIDWYTK:\n");
            sb.append("\t").append(doAppend(intro.getWidwytk()));

            sb.append("Kryptonite:\n");
            sb.append("\t").append(doAppend(intro.getKryptonite()));

            sb.append("What and When\n");
            sb.append("\t").append(doAppend(intro.getWhatAndWhen()));
        }
        
        if (personal == null) {
            sb.append("Personal not set\n\n");
        } else {
            sb.append("Personal:\n\n");

            sb.append("\tBest: ").append(doAppend(personal.getBest()));
            sb.append("\tWorst: ").append(doAppend(personal.getWorst()));
        }
        
        if (family == null) {
            sb.append("Family not set\n\n");
        } else {
            sb.append("Family:\n\n");

            sb.append("\tBest: ").append(doAppend(family.getBest()));
            sb.append("\tWorst: ").append(doAppend(family.getWorst()));
        }

        if (work == null) {
            sb.append("Work not set\n\n");
        } else {
            sb.append("Work:\n\n");

            sb.append("\tBest: ").append(doAppend(work.getBest()));
            sb.append("\tWorst: ").append(doAppend(work.getWorst()));
        }

        if (stats == null) {
            sb.append("Stats not set\n\n");
        } else {
            sb.append("Stats:\n\n");

            sb.append("\texercise: ")
                .append(((stats.getExercise() != null) ? stats.getExercise() : "not set"));
            sb.append(", gtg: ")
                .append(((stats.getGtg() != null) ? stats.getGtg() : "not set"));
            sb.append(", meditate: ")
                .append(((stats.getMeditate() != null) ? stats.getMeditate() : "not set"));
            sb.append(", meetings: ")
                .append(((stats.getMeetings() != null) ? stats.getMeetings() : "not set"));
            sb.append(", pray: ")
                .append(((stats.getPray() != null) ? stats.getPray() : "not set"));
            sb.append(", read: ")
                .append(((stats.getRead() != null) ? stats.getRead() : "not set"));
            sb.append(", sponsor: ")
                .append(((stats.getSponsor() != null) ? stats.getSponsor() : "not set")).append("\n\n");
        }

        return sb.toString();
    }

    private StringBuffer doAppend(String toAppend) {
        StringBuffer sb = new StringBuffer();
        return sb
            .append((toAppend != null) ? toAppend.replace("\n", "\n\t") : "not set")
            .append("\n\n");
    }
}
