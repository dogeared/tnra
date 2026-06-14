package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.router.BeforeEnterEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntitlementGateTest {

    private BillingClient billingClient;
    private OidcUserService oidc;
    private BeforeEnterEvent event;

    @BeforeEach
    void setUp() {
        billingClient = mock(BillingClient.class);
        oidc = mock(OidcUserService.class);
        event = mock(BeforeEnterEvent.class);
    }

    private EntitlementGate gate(Optional<BillingClient> client) {
        return new EntitlementGate(client, oidc);
    }

    @Test
    void billingDisabled_allowsEverything() {
        gate(Optional.empty()).beforeEnter(event);
        verify(event, never()).forwardTo(any(Class.class));
    }

    @Test
    void notAuthenticated_allows() {
        when(oidc.isAuthenticated()).thenReturn(false);
        gate(Optional.of(billingClient)).beforeEnter(event);
        verify(event, never()).forwardTo(any(Class.class));
    }

    @Test
    void allowedTarget_notGatedEvenWhenNotEntitled() {
        when(oidc.isAuthenticated()).thenReturn(true);
        doReturn(ProfileView.class).when(event).getNavigationTarget();

        gate(Optional.of(billingClient)).beforeEnter(event);

        verify(event, never()).forwardTo(any(Class.class));
        verify(billingClient, never()).isEntitled(any());
    }

    @Test
    void gatedTarget_notEntitled_forwardsToBilling() {
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn("m@x.com");
        doReturn(StatsView.class).when(event).getNavigationTarget();
        when(billingClient.isEntitled("m@x.com")).thenReturn(false);

        gate(Optional.of(billingClient)).beforeEnter(event);

        verify(event).forwardTo(BillingView.class);
    }

    @Test
    void gatedTarget_entitled_allows() {
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn("m@x.com");
        doReturn(StatsView.class).when(event).getNavigationTarget();
        when(billingClient.isEntitled("m@x.com")).thenReturn(true);

        gate(Optional.of(billingClient)).beforeEnter(event);

        verify(event, never()).forwardTo(any(Class.class));
    }

    @Test
    void nullEmail_allows() {
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn(null);
        doReturn(StatsView.class).when(event).getNavigationTarget();

        gate(Optional.of(billingClient)).beforeEnter(event);

        verify(event, never()).forwardTo(any(Class.class));
    }
}
