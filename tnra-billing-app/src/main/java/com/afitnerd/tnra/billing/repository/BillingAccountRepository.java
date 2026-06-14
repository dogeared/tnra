package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, Long> {

    /** Entitlement lookup and checkout dedup — the beneficiary key. */
    Optional<BillingAccount> findByGroupSlugAndEmail(String groupSlug, String email);

    /** Webhook routing — map a Lemon Squeezy subscription event back to the beneficiary account. */
    Optional<BillingAccount> findByLsSubscriptionId(String lsSubscriptionId);

    Optional<BillingAccount> findByLsCustomerId(String lsCustomerId);

    /** "Subscriptions I'm covering" — a payer's gift list within a group. */
    List<BillingAccount> findByGroupSlugAndPayerEmail(String groupSlug, String payerEmail);
}
