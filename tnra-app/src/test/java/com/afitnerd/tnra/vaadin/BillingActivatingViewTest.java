package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BillingActivatingViewTest {

    private OidcUserService authedAs(String email) {
        OidcUserService oidc = mock(OidcUserService.class);
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn(email);
        return oidc;
    }

    @Test
    void shouldPoll_onlyWhenBillingPresentAndAuthenticated() {
        BillingClient client = mock(BillingClient.class);
        assertTrue(new BillingActivatingView(Optional.of(client), authedAs("m@x.com")).shouldPoll());

        OidcUserService anon = mock(OidcUserService.class);
        when(anon.isAuthenticated()).thenReturn(false);
        assertFalse(new BillingActivatingView(Optional.of(client), anon).shouldPoll());
        assertFalse(new BillingActivatingView(Optional.empty(), authedAs("m@x.com")).shouldPoll());
    }

    @Test
    void entitledNow_pollsFreshEntitlement() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitledFresh("m@x.com")).thenReturn(false, true);
        BillingActivatingView view = new BillingActivatingView(Optional.of(client), authedAs("m@x.com"));

        assertFalse(view.entitledNow());
        assertTrue(view.entitledNow());
    }

    @Test
    void entitledNow_trueWhenBillingDisabled() {
        BillingActivatingView view = new BillingActivatingView(Optional.empty(), authedAs("m@x.com"));
        assertTrue(view.entitledNow());
    }

    @Test
    void tick_stopsAndForwardsOnceEntitled() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitledFresh("m@x.com")).thenReturn(true);
        BillingActivatingView view = new BillingActivatingView(Optional.of(client), authedAs("m@x.com"));

        assertTrue(view.tick());          // entitled → polling done
        assertEquals(1, view.polls);
        assertNull(view.continueButton);  // no timeout UI
    }

    @Test
    void tick_keepsPollingWhileNotEntitledAndUnderLimit() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitledFresh("m@x.com")).thenReturn(false);
        BillingActivatingView view = new BillingActivatingView(Optional.of(client), authedAs("m@x.com"));

        assertFalse(view.tick());         // not entitled, polls well under the cap → keep going
        assertNull(view.continueButton);
    }

    @Test
    void tick_timesOutAndShowsContinueAtMaxPolls() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitledFresh("m@x.com")).thenReturn(false);
        BillingActivatingView view = new BillingActivatingView(Optional.of(client), authedAs("m@x.com"));
        view.polls = BillingActivatingView.MAX_POLLS - 1; // next tick reaches the cap

        assertTrue(view.tick());          // gave up waiting
        assertNotNull(view.continueButton); // timeout UI offered
    }
}
