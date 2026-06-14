package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import com.afitnerd.tnra.billing.util.HashUtil;
import com.afitnerd.tnra.billing.util.TokenGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Registers a group centrally at provisioning time: creates its {@link GroupBilling} row, mints a
 * per-group API token (only the SHA-256 hash is stored), and applies the default free-trial window.
 * Also supports later admin updates of the group's exempt / trial settings (pilot, promo).
 */
@Service
public class GroupRegistrationService {

    private final GroupBillingRepository repository;
    private final BillingAppProperties props;
    private final Clock clock;

    public GroupRegistrationService(GroupBillingRepository repository,
                                    BillingAppProperties props,
                                    Clock clock) {
        this.repository = repository;
        this.props = props;
        this.clock = clock;
    }

    /** @return the PLAINTEXT token (shown once) plus the persisted row. */
    public RegistrationResult register(String groupSlug, Integer trialDays, Boolean exempt) {
        if (repository.findByGroupSlug(groupSlug).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Group already registered: " + groupSlug);
        }
        String token = TokenGenerator.newToken();
        GroupBilling group = new GroupBilling(groupSlug, HashUtil.sha256Hex(token));
        group.setExempt(Boolean.TRUE.equals(exempt));
        group.setCompUntil(trialEnd(trialDays != null ? trialDays : props.getTrialDays()));
        repository.save(group);
        return new RegistrationResult(token, group);
    }

    public GroupBilling update(String groupSlug, Boolean exempt, Integer trialDays) {
        GroupBilling group = repository.findByGroupSlug(groupSlug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Group not registered: " + groupSlug));
        if (exempt != null) {
            group.setExempt(exempt);
        }
        if (trialDays != null) {
            group.setCompUntil(trialEnd(trialDays));
        }
        repository.save(group);
        return group;
    }

    private LocalDateTime trialEnd(int days) {
        return days > 0 ? LocalDateTime.now(clock).plusDays(days) : null;
    }

    public record RegistrationResult(String token, GroupBilling group) {}
}
