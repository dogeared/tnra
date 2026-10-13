package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MembershipPromptTest {

    private static final String BASE = "https://g.tnra.app";

    @BeforeEach
    void setUp() {
        // Minimal UI so AppNotification.error(...) has somewhere to attach.
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
    void hiddenUntilRefreshed_andHiddenForEntitledMember() {
        MembershipPrompt prompt = new MembershipPrompt(clientWith(true, "ACTIVE"), "m@x.com", BASE + "/");
        assertFalse(prompt.isVisible());

        assertFalse(prompt.refresh());
        assertFalse(prompt.isVisible());
    }

    @Test
    void pending_showsPayOptions_noUpdateCard() {
        MembershipPrompt prompt = new MembershipPrompt(clientWith(false, "PENDING_PAYMENT"), "m@x.com", BASE);

        assertTrue(prompt.refresh());
        assertTrue(prompt.isVisible());
        assertEquals(MembershipPrompt.PENDING_TEXT, prompt.blurb.getText());
        assertTrue(prompt.payButtons.isVisible());
        assertFalse(prompt.updatePaymentButton.isVisible(), "nothing to update before a first subscription");
    }

    @Test
    void suspended_showsPayOptions_andPortal() {
        MembershipPrompt prompt = new MembershipPrompt(clientWith(false, "SUSPENDED"), "m@x.com", BASE);

        assertTrue(prompt.refresh());
        assertEquals(MembershipPrompt.SUSPENDED_TEXT, prompt.blurb.getText());
        assertTrue(prompt.payButtons.isVisible());
        assertTrue(prompt.updatePaymentButton.isVisible());
    }

    @Test
    void gracePeriod_stillEntitled_butWarnsAndLeadsWithUpdateCard() {
        MembershipPrompt prompt = new MembershipPrompt(clientWith(true, "ON_GRACE_PERIOD"), "m@x.com", BASE);

        assertTrue(prompt.refresh());
        assertTrue(prompt.isVisible());
        assertEquals(MembershipPrompt.GRACE_TEXT, prompt.blurb.getText());
        assertFalse(prompt.payButtons.isVisible(), "they have a subscription — fix the card, don't buy another");
        assertTrue(prompt.updatePaymentButton.isVisible());
    }

    @Test
    void refresh_failsClosedToHidden_onBillingError_orNoEmail() {
        BillingClient broken = mock(BillingClient.class);
        when(broken.entitlement(anyString())).thenThrow(new RuntimeException("central down"));
        MembershipPrompt prompt = new MembershipPrompt(broken, "m@x.com", BASE);
        assertFalse(prompt.refresh());
        assertFalse(prompt.isVisible());

        MembershipPrompt anon = new MembershipPrompt(mock(BillingClient.class), null, BASE);
        assertFalse(anon.refresh());
        assertFalse(anon.isVisible());
    }

    @Test
    void startCheckout_selfPay_withActivatingAsDone_andBillingPageAsReturn() {
        BillingClient client = clientWith(false, "PENDING_PAYMENT");
        when(client.createCheckout("m@x.com", "yearly", "m@x.com", BASE + "/billing/activating"))
            .thenReturn("https://tnra.app/pay?_ptxn=txn_1");
        MembershipPrompt prompt = new MembershipPrompt(client, "m@x.com", BASE);

        prompt.startCheckout("yearly");

        verify(client).createCheckout("m@x.com", "yearly", "m@x.com", BASE + "/billing/activating");
    }

    @Test
    void startCheckout_conflict_andGenericFailure_notifyWithoutThrowing() {
        BillingClient client = clientWith(false, "PENDING_PAYMENT");
        when(client.createCheckout(any(), any(), any(), any()))
            .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "dup", null, null, null))
            .thenThrow(new RuntimeException("boom"));
        MembershipPrompt prompt = new MembershipPrompt(client, "m@x.com", BASE);

        prompt.startCheckout("monthly"); // 409
        prompt.startCheckout("monthly"); // generic
        verify(client, Mockito.times(2)).createCheckout(any(), any(), any(), any());

        new MembershipPrompt(client, null, BASE).startCheckout("monthly"); // no email → no call
        verify(client, Mockito.times(2)).createCheckout(any(), any(), any(), any());
    }

    @Test
    void openPortal_callsClient_andSurvivesFailure() {
        BillingClient client = clientWith(true, "ON_GRACE_PERIOD");
        when(client.portalUrl("m@x.com")).thenReturn("https://portal/1").thenThrow(new RuntimeException("x"));
        MembershipPrompt prompt = new MembershipPrompt(client, "m@x.com", BASE);

        prompt.openPortal();
        prompt.openPortal();
        verify(client, Mockito.times(2)).portalUrl("m@x.com");

        new MembershipPrompt(client, null, BASE).openPortal();
        verify(client, never()).portalUrl((String) null);
    }
}
