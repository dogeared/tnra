package com.afitnerd.tnra.model;

import com.afitnerd.tnra.model.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "stat_definition")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "scope", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("GLOBAL")
public class StatDefinition {

    public enum StatType {
        NUMERIC, BOOLEAN, TEXT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(length = 10)
    private String emoji;

    @Enumerated(EnumType.STRING)
    @Column(name = "stat_type", nullable = false, length = 20)
    private StatType statType = StatType.NUMERIC;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean archived = false;

    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    public StatDefinition() {}

    public StatDefinition(String name, String label, String emoji, int displayOrder) {
        this.name = name;
        this.label = label;
        this.emoji = emoji;
        this.displayOrder = displayOrder;
        this.statType = StatType.NUMERIC;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public StatType getStatType() { return statType; }
    public void setStatType(StatType statType) { this.statType = statType; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
