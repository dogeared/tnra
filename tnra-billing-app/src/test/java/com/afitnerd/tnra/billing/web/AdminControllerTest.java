package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.service.GroupRegistrationService;
import com.afitnerd.tnra.billing.service.GroupRegistrationService.RegistrationResult;
import com.afitnerd.tnra.billing.web.dto.GroupBillingUpdateRequest;
import com.afitnerd.tnra.billing.web.dto.RegisterGroupRequest;
import com.afitnerd.tnra.billing.web.dto.RegisterGroupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    private static final String ADMIN = "admin-secret";

    private GroupRegistrationService service;
    private AdminController controller;

    @BeforeEach
    void setUp() {
        service = mock(GroupRegistrationService.class);
        BillingAppProperties props = new BillingAppProperties();
        props.setAdminToken(ADMIN);
        controller = new AdminController(service, props);
    }

    @Test
    void register_withValidAdminToken_returnsTokenAndNormalizesSlug() {
        GroupBilling g = new GroupBilling("rome", "hash");
        when(service.register(eq("rome"), any(), any())).thenReturn(new RegistrationResult("tok-123", g));

        RegisterGroupResponse resp = controller.register(ADMIN, new RegisterGroupRequest("ROME", 60, false));

        assertEquals("rome", resp.groupSlug());
        assertEquals("tok-123", resp.apiToken());
    }

    @Test
    void register_invalidAdminToken_unauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.register("wrong", new RegisterGroupRequest("rome", null, null)));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void register_missingAdminToken_unauthorized() {
        assertThrows(ResponseStatusException.class,
            () -> controller.register(null, new RegisterGroupRequest("rome", null, null)));
    }

    @Test
    void register_adminApiNotConfigured_unauthorized() {
        BillingAppProperties blank = new BillingAppProperties(); // adminToken ""
        AdminController c = new AdminController(service, blank);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> c.register("anything", new RegisterGroupRequest("rome", null, null)));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void register_blankSlug_badRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.register(ADMIN, new RegisterGroupRequest("  ", null, null)));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void update_withValidToken_returnsView() {
        GroupBilling g = new GroupBilling("rome", "hash");
        g.setExempt(true);
        when(service.update(eq("rome"), eq(true), any())).thenReturn(g);

        var view = controller.update(ADMIN, "rome", new GroupBillingUpdateRequest(true, null));

        assertEquals("rome", view.groupSlug());
        assertEquals(true, view.exempt());
    }
}
