package com.afitnerd.tnra.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "post_stat_value", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "stat_definition_id"})
})
public class PostStatValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    @JsonIgnoreProperties({"statValues", "user"})
    private Post post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "stat_definition_id")
    private StatDefinition statDefinition;

    @Column(name = "stat_value")
    private Integer value;

    public PostStatValue() {}

    public PostStatValue(Post post, StatDefinition statDefinition, Integer value) {
        this.post = post;
        this.statDefinition = statDefinition;
        this.value = value;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public StatDefinition getStatDefinition() { return statDefinition; }
    public void setStatDefinition(StatDefinition statDefinition) { this.statDefinition = statDefinition; }

    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}
