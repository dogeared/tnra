package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.GroupBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupBillingRepository extends JpaRepository<GroupBilling, String> {

    Optional<GroupBilling> findByGroupSlug(String groupSlug);

    /** Resolve an incoming per-group bearer token (already hashed) to its group. */
    Optional<GroupBilling> findByApiTokenHash(String apiTokenHash);

    /** Reminder sweep — groups whose trial / promo window ends inside the window. */
    java.util.List<GroupBilling> findByCompUntilBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);
}
