package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingViewTest {

    private OidcUserService authedAs(String email) {
        OidcUserService oidc = mock(OidcUserService.class);
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn(email);
        return oidc;
    }

    @Test
    void buildsPayAndUpdateButtonsWhenBillingEnabled() {
        BillingClient client = mock(BillingClient.class);
        BillingView view = new BillingView(Optional.of(client), authedAs("m@x.com"));

        assertNotNull(view.monthlyButton);
        assertNotNull(view.yearlyButton);
        assertNotNull(view.updatePaymentButton);
    }

    @Test
    void startCheckout_callsClientWithSelfPay() {
        BillingClient client = mock(BillingClient.class);
        when(client.createCheckout("m@x.com", "monthly", "m@x.com")).thenReturn("https://pay/1");
        BillingView view = new BillingView(Optional.of(client), authedAs("m@x.com"));

        view.startCheckout("monthly");

        verify(client).createCheckout("m@x.com", "monthly", "m@x.com");
    }

    @Test
    void openPortal_callsClient() {
        BillingClient client = mock(BillingClient.class);
        when(client.portalUrl("m@x.com")).thenReturn("https://portal/1");
        BillingView view = new BillingView(Optional.of(client), authedAs("m@x.com"));

        view.openPortal();

        verify(client).portalUrl("m@x.com");
    }

    @Test
    void billingDisabled_showsNotEnabled_noButtons() {
        BillingView view = new BillingView(Optional.empty(), mock(OidcUserService.class));

        assertNull(view.monthlyButton);
    }
}
