package com.afitnerd.tnra.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Group-level billing config, keyed by the immutable group slug (the same slug the provisioning
 * CLI registers). Holds the group-wide bypass facets (exempt = forever free, comp_until = trial
 * window) and the hashed per-group API token that scopes a caller to its own group.
 */
@Entity
@Table(name = "group_billing")
public class GroupBilling {

    @Id
    @Column(name = "group_slug", length = 64, nullable = false)
    private String groupSlug;

    @Column(nullable = false)
    private Boolean exempt = false;

    @Column(name = "comp_until")
    private LocalDateTime compUntil;

    @Column(name = "api_token_hash", length = 128, nullable = false)
    private String apiTokenHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (exempt == null) {
            exempt = false;
        }
    }

    public GroupBilling() {}

    public GroupBilling(String groupSlug, String apiTokenHash) {
        this.groupSlug = groupSlug;
        this.apiTokenHash = apiTokenHash;
    }

    public String getGroupSlug() {
        return groupSlug;
    }

    public void setGroupSlug(String groupSlug) {
        this.groupSlug = groupSlug;
    }

    public Boolean getExempt() {
        return exempt;
    }

    public void setExempt(Boolean exempt) {
        this.exempt = exempt;
    }

    public LocalDateTime getCompUntil() {
        return compUntil;
    }

    public void setCompUntil(LocalDateTime compUntil) {
        this.compUntil = compUntil;
    }

    public String getApiTokenHash() {
        return apiTokenHash;
    }

    public void setApiTokenHash(String apiTokenHash) {
        this.apiTokenHash = apiTokenHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
