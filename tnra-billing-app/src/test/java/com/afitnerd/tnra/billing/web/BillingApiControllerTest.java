package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.service.CheckoutService;
import com.afitnerd.tnra.billing.service.EntitlementResult;
import com.afitnerd.tnra.billing.service.EntitlementService;
import com.afitnerd.tnra.billing.web.dto.CheckoutRequest;
import com.afitnerd.tnra.billing.web.dto.CheckoutResponse;
import com.afitnerd.tnra.billing.web.dto.CoveringEntry;
import com.afitnerd.tnra.billing.web.dto.EntitlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BillingApiControllerTest {

    private final Principal rome = () -> "rome";

    private EntitlementService entitlementService;
    private CheckoutService checkoutService;
    private BillingApiController controller;

    @BeforeEach
    void setUp() {
        entitlementService = mock(EntitlementService.class);
        checkoutService = mock(CheckoutService.class);
        controller = new BillingApiController(entitlementService, checkoutService);
    }

    @Test
    void entitlement_mapsResult() {
        when(entitlementService.isEntitled("rome", "m@x.com"))
            .thenReturn(EntitlementResult.entitled(BillingStatus.ACTIVE, "SUBSCRIPTION"));

        EntitlementResponse resp = controller.entitlement(rome, "m@x.com");

        assertTrue(resp.entitled());
        assertEquals("ACTIVE", resp.status());
        assertEquals("SUBSCRIPTION", resp.reason());
    }

    @Test
    void checkout_delegatesWithGroupFromPrincipal() {
        CheckoutRequest req = new CheckoutRequest("m@x.com", "monthly", null);
        when(checkoutService.createCheckout("rome", req)).thenReturn("https://pay/1");

        CheckoutResponse resp = controller.checkout(rome, req);

        assertEquals("https://pay/1", resp.url());
    }

    @Test
    void covering_delegates() {
        when(checkoutService.covering("rome", "admin@x.com"))
            .thenReturn(List.of(new CoveringEntry("ben@x.com", "ACTIVE")));

        List<CoveringEntry> resp = controller.covering(rome, "admin@x.com");

        assertEquals(1, resp.size());
    }

    @Test
    void portal_delegates() {
        when(checkoutService.portalUrl("rome", "m@x.com")).thenReturn("https://portal/1");

        assertEquals("https://portal/1", controller.portal(rome, "m@x.com").url());
    }
}
