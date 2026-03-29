package com.afitnerd.tnra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    private Date start;
    private Date finish;
    private PostState state;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "widwytk", column = @Column(columnDefinition = "TEXT")),
        @AttributeOverride(name = "kryptonite", column = @Column(columnDefinition = "TEXT")),
        @AttributeOverride(name = "whatAndWhen", column = @Column(columnDefinition = "TEXT"))
    })
    private Intro intro;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "personal_best", columnDefinition = "TEXT")),
        @AttributeOverride(name = "worst", column = @Column(name = "personal_worst", columnDefinition = "TEXT"))
    })
    private Category personal;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "family_best", columnDefinition = "TEXT")),
        @AttributeOverride(name = "worst", column = @Column(name = "family_worst", columnDefinition = "TEXT"))
    })
    private Category family;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "best", column = @Column(name = "work_best", columnDefinition = "TEXT")),
        @AttributeOverride(name = "worst", column = @Column(name = "work_worst", columnDefinition = "TEXT"))
    })
    private Category work;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("post")
    private List<PostStatValue> statValues = new ArrayList<>();

    @ManyToOne(optional = false)
    @JsonIgnoreProperties("posts")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
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

    public List<PostStatValue> getStatValues() {
        return statValues;
    }

    public void setStatValues(List<PostStatValue> statValues) {
        this.statValues = statValues;
    }

    public Integer getStatValue(String statName) {
        return statValues.stream()
            .filter(sv -> sv.getStatDefinition().getName().equals(statName))
            .map(PostStatValue::getValue)
            .findFirst()
            .orElse(null);
    }

    public void setStatValue(StatDefinition statDef, Integer value) {
        if (statDef == null || statDef.getId() == null) {
            throw new IllegalArgumentException("StatDefinition and its ID must not be null");
        }
        PostStatValue existing = statValues.stream()
            .filter(sv -> sv.getStatDefinition().getId().equals(statDef.getId()))
            .findFirst()
            .orElse(null);

        if (existing != null) {
            existing.setValue(value);
        } else {
            PostStatValue psv = new PostStatValue(this, statDef, value);
            statValues.add(psv);
        }
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
            sb.append("*Intro not set*\n\n");
        } else {
            sb.append("*Intro:*\n\n");
            sb.append("*WIDWYTK:*\n");
            sb.append("\t").append(doAppend(intro.getWidwytk()));
            sb.append("*Kryptonite:*\n");
            sb.append("\t").append(doAppend(intro.getKryptonite()));
            sb.append("*What and When:*\n");
            sb.append("\t").append(doAppend(intro.getWhatAndWhen()));
        }

        if (personal == null) {
            sb.append("*Personal not set*\n\n");
        } else {
            sb.append("*Personal:*\n\n");
            sb.append("\t*Best:* ").append(doAppend(personal.getBest()));
            sb.append("\t*Worst:* ").append(doAppend(personal.getWorst()));
        }

        if (family == null) {
            sb.append("*Family not set*\n\n");
        } else {
            sb.append("*Family:*\n\n");
            sb.append("\t*Best:* ").append(doAppend(family.getBest()));
            sb.append("\t*Worst:* ").append(doAppend(family.getWorst()));
        }

        if (work == null) {
            sb.append("*Work not set*\n\n");
        } else {
            sb.append("*Work:*\n\n");
            sb.append("\t*Best:* ").append(doAppend(work.getBest()));
            sb.append("\t*Worst:* ").append(doAppend(work.getWorst()));
        }

        if (statValues == null || statValues.isEmpty()) {
            sb.append("*Stats not set*\n\n");
        } else {
            sb.append("*Stats:*\n\n");
            statValues.stream()
                .sorted((a, b) -> {
                    boolean aGlobal = !(a.getStatDefinition() instanceof PersonalStatDefinition);
                    boolean bGlobal = !(b.getStatDefinition() instanceof PersonalStatDefinition);
                    if (aGlobal != bGlobal) return aGlobal ? -1 : 1;
                    return a.getStatDefinition().getDisplayOrder().compareTo(b.getStatDefinition().getDisplayOrder());
                })
                .forEach(sv -> {
                    sb.append("\t*").append(sv.getStatDefinition().getLabel()).append(":* ");
                    sb.append(sv.getValue() != null ? sv.getValue() : "not set");
                    sb.append("\n");
                });
            sb.append("\n");
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
