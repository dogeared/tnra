package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * The single entitlement gate, most-permissive-wins. A member is entitled if ANY of these hold,
 * checked in this order (the first true one is the reported reason):
 *
 * <pre>
 *   1. group exempt              GROUP_EXEMPT      (pilot group, forever free)
 *   2. member exempt             MEMBER_EXEMPT     (individual permanent comp)
 *   3. group comp_until > now    GROUP_TRIAL       (whole-group trial / promo)
 *   4. member comp_until > now   MEMBER_COMP       (individual time-boxed waiver)
 *   5. status ACTIVE/GRACE       SUBSCRIPTION      (paying, incl. dunning grace)
 *   else                         not entitled      (PENDING_PAYMENT / SUSPENDED / no account)
 * </pre>
 *
 * If the group isn't registered centrally the caller is misconfigured; we deny with
 * GROUP_NOT_REGISTERED rather than guess. (The per-group app fails OPEN on transport errors, so a
 * central outage never reaches this method.)
 */
@Service
public class EntitlementService {

    private final GroupBillingRepository groupBillingRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final Clock clock;

    public EntitlementService(GroupBillingRepository groupBillingRepository,
                              BillingAccountRepository billingAccountRepository,
                              Clock clock) {
        this.groupBillingRepository = groupBillingRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.clock = clock;
    }

    public EntitlementResult isEntitled(String groupSlug, String email) {
        GroupBilling group = groupBillingRepository.findByGroupSlug(groupSlug).orElse(null);
        if (group == null) {
            return EntitlementResult.denied(BillingStatus.PENDING_PAYMENT, "GROUP_NOT_REGISTERED");
        }

        BillingAccount account = billingAccountRepository
            .findByGroupSlugAndEmail(groupSlug, email)
            .orElse(null);
        BillingStatus status = account != null ? account.getStatus() : BillingStatus.PENDING_PAYMENT;
        // Who pays for this member — set only on a gift; lets the group app show "gifted by …".
        String payer = account != null ? account.getPayerEmail() : null;

        LocalDateTime now = LocalDateTime.now(clock);

        if (Boolean.TRUE.equals(group.getExempt())) {
            return EntitlementResult.entitled(status, "GROUP_EXEMPT", payer);
        }
        if (account != null && Boolean.TRUE.equals(account.getExempt())) {
            return EntitlementResult.entitled(status, "MEMBER_EXEMPT", payer);
        }
        if (isFuture(group.getCompUntil(), now)) {
            return EntitlementResult.entitled(status, "GROUP_TRIAL", payer);
        }
        if (account != null && isFuture(account.getCompUntil(), now)) {
            return EntitlementResult.entitled(status, "MEMBER_COMP", payer);
        }
        if (status == BillingStatus.ACTIVE || status == BillingStatus.ON_GRACE_PERIOD) {
            return EntitlementResult.entitled(status, "SUBSCRIPTION", payer);
        }
        return EntitlementResult.denied(status, "NOT_ENTITLED");
    }

    private boolean isFuture(LocalDateTime when, LocalDateTime now) {
        return when != null && when.isAfter(now);
    }
}
