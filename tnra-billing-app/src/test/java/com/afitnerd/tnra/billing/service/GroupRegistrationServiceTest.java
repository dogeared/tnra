package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.repository.GroupBillingRepository;
import com.afitnerd.tnra.billing.service.GroupRegistrationService.RegistrationResult;
import com.afitnerd.tnra.billing.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupRegistrationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-14T12:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private GroupBillingRepository repo;
    private GroupRegistrationService service;

    @BeforeEach
    void setUp() {
        repo = mock(GroupBillingRepository.class);
        BillingAppProperties props = new BillingAppProperties();
        props.setTrialDays(60);
        service = new GroupRegistrationService(repo, props, CLOCK);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_createsRowWithHashedToken_andDefaultTrial() {
        when(repo.findByGroupSlug("rome")).thenReturn(Optional.empty());

        RegistrationResult result = service.register("rome", null, null);

        assertEquals(43, result.token().length());
        ArgumentCaptor<GroupBilling> saved = ArgumentCaptor.forClass(GroupBilling.class);
        org.mockito.Mockito.verify(repo).save(saved.capture());
        GroupBilling g = saved.getValue();
        assertEquals(HashUtil.sha256Hex(result.token()), g.getApiTokenHash()); // only hash stored
        assertEquals(Boolean.FALSE, g.getExempt());
        assertEquals(NOW.plusDays(60), g.getCompUntil());
    }

    @Test
    void register_honorsExplicitTrialAndExempt() {
        when(repo.findByGroupSlug("pilot")).thenReturn(Optional.empty());

        RegistrationResult result = service.register("pilot", 14, true);

        assertEquals(NOW.plusDays(14), result.group().getCompUntil());
        assertTrue(result.group().getExempt());
    }

    @Test
    void register_zeroTrial_meansNoCompWindow() {
        when(repo.findByGroupSlug("g")).thenReturn(Optional.empty());

        RegistrationResult result = service.register("g", 0, false);

        assertNull(result.group().getCompUntil());
    }

    @Test
    void register_duplicate_conflict() {
        when(repo.findByGroupSlug("rome")).thenReturn(Optional.of(new GroupBilling("rome", "h")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.register("rome", null, null));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void update_setsExemptAndExtendsTrial() {
        GroupBilling g = new GroupBilling("rome", "h");
        when(repo.findByGroupSlug("rome")).thenReturn(Optional.of(g));

        GroupBilling updated = service.update("rome", true, 30);

        assertTrue(updated.getExempt());
        assertEquals(NOW.plusDays(30), updated.getCompUntil());
    }

    @Test
    void update_missingGroup_notFound() {
        when(repo.findByGroupSlug("nope")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> service.update("nope", true, null));
        assertEquals(404, ex.getStatusCode().value());
    }
}
