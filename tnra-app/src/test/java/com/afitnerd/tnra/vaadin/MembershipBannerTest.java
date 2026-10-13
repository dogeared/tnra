package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipBannerTest {

    @BeforeEach
    void setUp() {
        UI ui = new UI();
        VaadinSession session = mock(VaadinSession.class, Mockito.RETURNS_DEEP_STUBS);
        lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        lenient().when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private static BillingClient clientWith(boolean entitled, String status) {
        BillingClient client = mock(BillingClient.class);
        when(client.entitlement("m@x.com")).thenReturn(new BillingClient.Entitlement(entitled, status, null));
        return client;
    }

    @Test
    void shownOnlyInGracePeriod() {
        MembershipBanner banner = new MembershipBanner("https://g.tnra.app/");
        assertFalse(banner.isVisible(), "hidden until refreshed");

        assertTrue(banner.refresh(clientWith(true, "ON_GRACE_PERIOD"), "m@x.com"));
        assertTrue(banner.isVisible());

        assertFalse(banner.refresh(clientWith(true, "ACTIVE"), "m@x.com"));
        assertFalse(banner.isVisible());
        assertFalse(banner.refresh(clientWith(false, "PENDING_PAYMENT"), "m@x.com"));
        assertFalse(banner.refresh(clientWith(false, "SUSPENDED"), "m@x.com"));
    }

    @Test
    void failsClosedToHidden() {
        MembershipBanner banner = new MembershipBanner("https://g.tnra.app/");
        BillingClient broken = mock(BillingClient.class);
        when(broken.entitlement(anyString())).thenThrow(new RuntimeException("down"));
        assertFalse(banner.refresh(broken, "m@x.com"));

        BillingClient nullResp = mock(BillingClient.class);
        when(nullResp.entitlement(anyString())).thenReturn(null);
        assertFalse(banner.refresh(nullResp, "m@x.com"));

        assertFalse(banner.refresh(null, "m@x.com"));
        assertFalse(banner.refresh(mock(BillingClient.class), null));
        assertFalse(banner.isVisible());
    }

    @Test
    void startCardUpdate_usesOverlayUrl_andSurvivesFailure_andNoopsWithoutMember() {
        BillingClient client = clientWith(true, "ON_GRACE_PERIOD");
        when(client.paymentMethodUpdateUrl("m@x.com"))
            .thenReturn("https://tnra.app/pay?_ptxn=txn_u").thenThrow(new RuntimeException("x"));
        MembershipBanner banner = new MembershipBanner("https://g.tnra.app/");

        banner.startCardUpdate(); // before refresh: no member yet
        verify(client, never()).paymentMethodUpdateUrl(anyString());

        banner.refresh(client, "m@x.com");
        banner.startCardUpdate();
        banner.startCardUpdate(); // throws inside → notification, no exception out
        verify(client, times(2)).paymentMethodUpdateUrl("m@x.com");
        verify(client, never()).portalUrl(anyString()); // the full-page portal is NOT the card-update path
    }
}
