package com.afitnerd.tnra.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * Durable record of every Lemon Squeezy webhook received. {@code lsEventId} is unique for
 * idempotency (a duplicate delivery is a no-op). The full {@code rawPayload} is stored so an
 * event that can't be matched to an account (or fails processing) is never lost — it can be
 * reconciled or replayed rather than silently dropped.
 */
@Entity
@Table(
    name = "billing_webhook_event",
    uniqueConstraints = @UniqueConstraint(name = "uq_ls_event_id", columnNames = "ls_event_id")
)
public class BillingWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ls_event_id", length = 128, nullable = false)
    private String lsEventId;

    @Column(name = "event_name", length = 64, nullable = false)
    private String eventName;

    @Lob
    @Column(name = "raw_payload", nullable = false)
    private String rawPayload;

    /** Null when the event could not be matched to a billing account (needs reconciliation). */
    @Column(name = "matched_account_id")
    private Long matchedAccountId;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(length = 1000)
    private String error;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (processed == null) {
            processed = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLsEventId() {
        return lsEventId;
    }

    public void setLsEventId(String lsEventId) {
        this.lsEventId = lsEventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Long getMatchedAccountId() {
        return matchedAccountId;
    }

    public void setMatchedAccountId(Long matchedAccountId) {
        this.matchedAccountId = matchedAccountId;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
