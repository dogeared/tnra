package com.afitnerd.tnra.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * One member's billing account within a group. {@code email} is the BENEFICIARY (who is entitled);
 * {@code payerEmail} is who pays — null means self-pay, a different value means this subscription was
 * gifted by that person. Entitlement is always beneficiary-keyed; gift vs self-pay is irrelevant to it.
 */
@Entity
@Table(
    name = "billing_account",
    uniqueConstraints = @UniqueConstraint(name = "uq_group_email", columnNames = {"group_slug", "email"}),
    indexes = {
        @Index(name = "idx_subscription", columnList = "ls_subscription_id"),
        @Index(name = "idx_customer", columnList = "ls_customer_id"),
        @Index(name = "idx_payer", columnList = "group_slug, payer_email")
    }
)
public class BillingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_slug", length = 64, nullable = false)
    private String groupSlug;

    /** The beneficiary — who this account entitles. */
    @Column(length = 255, nullable = false)
    private String email;

    /** Who pays. Null = self-pay; set = gifted by this person. */
    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private BillingStatus status = BillingStatus.PENDING_PAYMENT;

    @Column(nullable = false)
    private Boolean exempt = false;

    @Column(name = "comp_until")
    private LocalDateTime compUntil;

    @Column(name = "ls_customer_id", length = 64)
    private String lsCustomerId;

    @Column(name = "ls_subscription_id", length = 64)
    private String lsSubscriptionId;

    @Column(name = "ls_variant", length = 16)
    private String lsVariant;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = BillingStatus.PENDING_PAYMENT;
        }
        if (exempt == null) {
            exempt = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** True when someone other than the beneficiary is paying for this account. */
    public boolean isGift() {
        return payerEmail != null && !payerEmail.equalsIgnoreCase(email);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupSlug() {
        return groupSlug;
    }

    public void setGroupSlug(String groupSlug) {
        this.groupSlug = groupSlug;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public BillingStatus getStatus() {
        return status;
    }

    public void setStatus(BillingStatus status) {
        this.status = status;
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

    public String getLsCustomerId() {
        return lsCustomerId;
    }

    public void setLsCustomerId(String lsCustomerId) {
        this.lsCustomerId = lsCustomerId;
    }

    public String getLsSubscriptionId() {
        return lsSubscriptionId;
    }

    public void setLsSubscriptionId(String lsSubscriptionId) {
        this.lsSubscriptionId = lsSubscriptionId;
    }

    public String getLsVariant() {
        return lsVariant;
    }

    public void setLsVariant(String lsVariant) {
        this.lsVariant = lsVariant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
