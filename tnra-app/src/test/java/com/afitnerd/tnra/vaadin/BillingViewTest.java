package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingViewTest {

    private static final String BASE = "http://localhost:8080";

    private UI ui;

    @BeforeEach
    void setUp() {
        // Minimal UI so Notification.show(...) in the gift guards has a current UI to attach to.
        ui = new UI();
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

    private OidcUserService authedAs(String email) {
        OidcUserService oidc = mock(OidcUserService.class);
        when(oidc.isAuthenticated()).thenReturn(true);
        when(oidc.getEmail()).thenReturn(email);
        return oidc;
    }

    private BillingView view(BillingClient client, UserService users, String email) {
        return new BillingView(Optional.of(client), authedAs(email), users, BASE);
    }

    private BeforeEnterEvent giftEvent(String giftEmail) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(
            new Location("billing", QueryParameters.simple(Map.of("gift", giftEmail))));
        return event;
    }

    @Test
    void buildsPayAndUpdateButtonsWhenBillingEnabled() {
        BillingView view = view(mock(BillingClient.class), mock(UserService.class), "m@x.com");

        assertNotNull(view.monthlyButton);
        assertNotNull(view.yearlyButton);
        assertNotNull(view.updatePaymentButton);
    }

    @Test
    void startCheckout_callsClientWithSelfPay() {
        BillingClient client = mock(BillingClient.class);
        when(client.createCheckout("m@x.com", "monthly", "m@x.com", BASE + "/billing/activating"))
            .thenReturn("https://pay/1");
        BillingView view = view(client, mock(UserService.class), "m@x.com");

        view.startCheckout("monthly");

        // self-pay, with the post-checkout "activating" page as the LS redirect target
        verify(client).createCheckout("m@x.com", "monthly", "m@x.com", BASE + "/billing/activating");
    }

    @Test
    void openPortal_callsClient() {
        BillingClient client = mock(BillingClient.class);
        when(client.portalUrl("m@x.com")).thenReturn("https://portal/1");
        BillingView view = view(client, mock(UserService.class), "m@x.com");

        view.openPortal();

        verify(client).portalUrl("m@x.com");
    }

    @Test
    void coveringSection_shownWhenPayingForOthers() {
        BillingClient client = mock(BillingClient.class);
        when(client.covering("payer@x.com")).thenReturn(List.of(
            new BillingClient.CoveredMember("ben@x.com", "ACTIVE")));
        BillingView view = view(client, mock(UserService.class), "payer@x.com");

        assertTrue(view.coveringSection.isVisible());
    }

    @Test
    void coveringSection_hiddenWhenNotCoveringAnyone() {
        BillingClient client = mock(BillingClient.class);
        when(client.covering("payer@x.com")).thenReturn(List.of());
        BillingView view = view(client, mock(UserService.class), "payer@x.com");

        assertFalse(view.coveringSection.isVisible());
    }

    @Test
    void coveringSection_hiddenInGiftMode() {
        BillingClient client = mock(BillingClient.class);
        when(client.covering("payer@x.com")).thenReturn(List.of(
            new BillingClient.CoveredMember("ben@x.com", "ACTIVE")));
        when(client.isEntitled("payer@x.com")).thenReturn(true);
        when(client.entitlement("dana@x.com")).thenReturn(new BillingClient.Entitlement(false, "", null));
        UserService users = mock(UserService.class);
        User dana = new User();
        dana.setEmail("dana@x.com");
        when(users.getUserByEmail("dana@x.com")).thenReturn(dana);
        BillingView view = view(client, users, "payer@x.com");
        assertTrue(view.coveringSection.isVisible()); // visible in self-pay mode

        view.beforeEnter(giftEvent("dana@x.com"));     // ...hidden once gifting a different member

        assertTrue(view.giftMode);
        assertFalse(view.coveringSection.isVisible());
    }

    @Test
    void billingDisabled_showsNotEnabled_noButtons() {
        BillingView view = new BillingView(
            Optional.empty(), mock(OidcUserService.class), mock(UserService.class), BASE);

        assertNull(view.monthlyButton);
    }

    @Test
    void giftMode_chargesPayerForBeneficiary_andReturnsToAppHome() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitled("payer@x.com")).thenReturn(true);
        UserService users = mock(UserService.class);
        User ben = new User();
        ben.setEmail("ben@x.com");
        ben.setFirstName("Ben");
        ben.setLastName("Z");
        when(users.getUserByEmail("ben@x.com")).thenReturn(ben);
        when(client.entitlement("ben@x.com")).thenReturn(new BillingClient.Entitlement(false, "", null));
        BillingView view = view(client, users, "payer@x.com");

        view.beforeEnter(giftEvent("ben@x.com"));

        assertTrue(view.giftMode);
        assertFalse(view.updatePaymentButton.isVisible()); // manage-your-own-sub hidden in gift mode

        when(client.createCheckout("ben@x.com", "monthly", "payer@x.com", BASE)).thenReturn("https://pay/9");
        view.startCheckout("monthly");
        // payer is charged for the beneficiary; gift returns to the app home, not the activating page
        verify(client).createCheckout("ben@x.com", "monthly", "payer@x.com", BASE);
    }

    @Test
    void giftMode_blockedWhenBeneficiaryAlreadyCovered() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitled("payer@x.com")).thenReturn(true);  // gifter is active
        // ...but the recipient is already settled (active subscription)
        when(client.entitlement("ben@x.com")).thenReturn(new BillingClient.Entitlement(true, "ACTIVE", null));
        UserService users = mock(UserService.class);
        User ben = new User();
        ben.setEmail("ben@x.com");
        when(users.getUserByEmail("ben@x.com")).thenReturn(ben);
        BillingView view = view(client, users, "payer@x.com");

        view.beforeEnter(giftEvent("ben@x.com"));

        assertFalse(view.giftMode);                       // no gift checkout offered
        assertFalse(view.monthlyButton.isEnabled());      // pay buttons disabled
        assertFalse(view.yearlyButton.isEnabled());
    }

    @Test
    void giftMode_allowedForGracePeriodBeneficiary_rescue() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitled("payer@x.com")).thenReturn(true);
        // Recipient is in dunning (failing their own payment) — giftable as a rescue.
        when(client.entitlement("ben@x.com"))
            .thenReturn(new BillingClient.Entitlement(true, "ON_GRACE_PERIOD", null));
        UserService users = mock(UserService.class);
        User ben = new User();
        ben.setEmail("ben@x.com");
        when(users.getUserByEmail("ben@x.com")).thenReturn(ben);
        BillingView view = view(client, users, "payer@x.com");

        view.beforeEnter(giftEvent("ben@x.com"));

        assertTrue(view.giftMode);
        when(client.createCheckout("ben@x.com", "monthly", "payer@x.com", BASE)).thenReturn("https://pay/r");
        view.startCheckout("monthly");
        verify(client).createCheckout("ben@x.com", "monthly", "payer@x.com", BASE);
    }

    @Test
    void giftMode_refusedWhenPayerNotEntitled_staysSelfPay() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitled("payer@x.com")).thenReturn(false);
        BillingView view = view(client, mock(UserService.class), "payer@x.com");

        view.beforeEnter(giftEvent("ben@x.com"));

        assertFalse(view.giftMode); // a non-entitled member cannot gift
        when(client.createCheckout("payer@x.com", "monthly", "payer@x.com", BASE + "/billing/activating"))
            .thenReturn("https://pay/self");
        view.startCheckout("monthly");
        verify(client).createCheckout("payer@x.com", "monthly", "payer@x.com", BASE + "/billing/activating");
    }

    @Test
    void giftMode_ignoredWhenRecipientNotAMember() {
        BillingClient client = mock(BillingClient.class);
        when(client.isEntitled("payer@x.com")).thenReturn(true);
        UserService users = mock(UserService.class);
        when(users.getUserByEmail("ghost@x.com")).thenReturn(null);
        BillingView view = view(client, users, "payer@x.com");

        view.beforeEnter(giftEvent("ghost@x.com"));

        assertFalse(view.giftMode);
    }

    @Test
    void giftToSelf_isTreatedAsSelfPay() {
        BillingClient client = mock(BillingClient.class);
        BillingView view = view(client, mock(UserService.class), "me@x.com");

        view.beforeEnter(giftEvent("me@x.com")); // gifting to yourself — ignored

        assertFalse(view.giftMode);
    }
}
