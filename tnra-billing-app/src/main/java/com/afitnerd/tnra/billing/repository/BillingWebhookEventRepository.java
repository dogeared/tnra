package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.BillingWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, Long> {

    /** Idempotency guard — a redelivered Lemon Squeezy event is a no-op. */
    boolean existsByLsEventId(String lsEventId);
}
