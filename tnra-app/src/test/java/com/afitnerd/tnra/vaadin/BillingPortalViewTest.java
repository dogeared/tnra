package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BillingPortalViewTest {

    private static OidcUserService authedAs(String email) {
        OidcUserService oidc = mock(OidcUserService.class);
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn(email);
        return oidc;
    }

    @Test
    void explainsWhereTheMemberIsGoing_andCountsDown() {
        BillingClient client = mock(BillingClient.class);
        when(client.portalUrl("m@x.com")).thenReturn("https://sandbox-customer-portal.paddle.com/x");
        BillingPortalView view = new BillingPortalView(Optional.of(client), authedAs("m@x.com"));

        view.resolvePortalUrl();

        assertEquals("https://sandbox-customer-portal.paddle.com/x", view.portalUrl);
        assertTrue(view.status.getText().contains("Paddle"));
        assertTrue(view.status.getText().contains("paddle.com"));
        assertTrue(view.countdownLine.isVisible());
        assertTrue(view.goNowButton.isVisible());
        assertEquals(String.valueOf(BillingPortalView.COUNTDOWN_SECONDS), view.countdown.getText());
    }

    @Test
    void noSubscription_explainsAndStays() {
        BillingClient client = mock(BillingClient.class);
        when(client.portalUrl("m@x.com"))
            .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "none", null, null, null));
        BillingPortalView view = new BillingPortalView(Optional.of(client), authedAs("m@x.com"));

        view.resolvePortalUrl();

        assertNull(view.portalUrl);
        assertEquals(BillingPortalView.NO_SUBSCRIPTION, view.status.getText());
        assertFalse(view.countdownLine.isVisible());
        assertFalse(view.goNowButton.isVisible());
        assertTrue(view.stayButton.isVisible());
    }

    @Test
    void billingDown_orBillingOff_orAnonymous_staysWithExplanation() {
        BillingClient broken = mock(BillingClient.class);
        when(broken.portalUrl("m@x.com")).thenThrow(new RuntimeException("central down"));
        BillingPortalView down = new BillingPortalView(Optional.of(broken), authedAs("m@x.com"));
        down.resolvePortalUrl();
        assertNull(down.portalUrl);
        assertEquals(BillingPortalView.UNAVAILABLE, down.status.getText());

        BillingPortalView off = new BillingPortalView(Optional.empty(), authedAs("m@x.com"));
        off.resolvePortalUrl();
        assertNull(off.portalUrl);

        OidcUserService anon = mock(OidcUserService.class);
        when(anon.isAuthenticated()).thenReturn(false);
        BillingPortalView anonymous = new BillingPortalView(Optional.of(mock(BillingClient.class)), anon);
        anonymous.resolvePortalUrl();
        assertNull(anonymous.portalUrl);
    }

    @Test
    void goNow_andStay_areSafeWithoutUi() {
        BillingPortalView view = new BillingPortalView(Optional.empty(), authedAs("m@x.com"));
        view.goNow();  // no url, no UI → no-op
        view.stay();   // no UI → no-op
        assertNull(view.portalUrl);
    }
}
