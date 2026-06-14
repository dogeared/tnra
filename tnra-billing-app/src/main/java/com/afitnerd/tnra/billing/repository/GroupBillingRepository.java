package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.GroupBilling;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupBillingRepository extends JpaRepository<GroupBilling, String> {

    Optional<GroupBilling> findByGroupSlug(String groupSlug);
}
