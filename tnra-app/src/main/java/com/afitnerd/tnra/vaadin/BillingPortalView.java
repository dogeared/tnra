package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Interstitial before the Paddle customer portal. The portal is a full-page site on paddle.com (Paddle
 * offers no custom domain for it), and landing on an unfamiliar site with no warning is jarring — so this
 * page says where the member is going and why, counts down a few seconds, and lets them go now or stay.
 * Invoices, cancellation and pausing live only in the portal; card updates use the in-app overlay instead.
 */
@Route(value = "billing/portal", layout = MainLayout.class)
@PermitAll
@PageTitle("Payment portal | TNRA")
public class BillingPortalView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(BillingPortalView.class);

    static final int COUNTDOWN_SECONDS = 6;
    static final String UNAVAILABLE =
        "We couldn\'t open the payment portal right now. Please try again in a moment.";
    static final String NO_SUBSCRIPTION =
        "There\'s no subscription to manage yet. Once your membership is active, invoices and "
        + "cancellation live in Paddle\'s portal.";

    private final transient Optional<BillingClient> billingClient;
    private final String email;

    final Paragraph status = new Paragraph();
    final Span countdown = new Span(String.valueOf(COUNTDOWN_SECONDS));
    final Paragraph countdownLine = new Paragraph();
    final Button goNowButton = new Button("Go to Paddle now", e -> goNow());
    final Button stayButton = new Button("Stay here", e -> stay());
    String portalUrl; // package-private for testing

    public BillingPortalView(Optional<BillingClient> billingClient, OidcUserService oidcUserService) {
        this.billingClient = billingClient;
        this.email = oidcUserService.isAuthenticated() ? oidcUserService.getEmail() : null;
        setAlignItems(Alignment.CENTER);

        status.setText("Paddle is our payment partner (the Merchant of Record for TNRA memberships). "
            + "Invoices, cancellation and subscription changes live in their secure customer portal, "
            + "which opens on paddle.com. You\'ll be signed in automatically.");
        status.setMaxWidth("40rem");
        status.getStyle().set("text-align", "center");
        countdownLine.add(new Span("Taking you there in "), countdown, new Span(" seconds…"));
        goNowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout buttons = new HorizontalLayout(goNowButton, stayButton);

        add(new H2("Heading to Paddle"), status, countdownLine, buttons);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        resolvePortalUrl();
        if (portalUrl == null) {
            return;
        }
        // Countdown + redirect run in the browser; the timer handle is parked on window so navigating away
        // (Stay here) can cancel it — a Vaadin route change doesn\'t reload the page.
        getElement().executeJs(
            "const el = $0, url = $1; let n = $2;"
            + "window.__tnraPortalTimer = setInterval(function () {"
            + "  n--; el.textContent = n;"
            + "  if (n <= 0) { clearInterval(window.__tnraPortalTimer); window.location.href = url; }"
            + "}, 1000);",
            countdown.getElement(), portalUrl, COUNTDOWN_SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        detachEvent.getUI().getPage().executeJs(
            "if (window.__tnraPortalTimer) { clearInterval(window.__tnraPortalTimer); }");
        super.onDetach(detachEvent);
    }

    /** Ask the billing service for a portal session; on any failure stay on this page with an explanation. */
    void resolvePortalUrl() {
        if (billingClient.isEmpty() || email == null) {
            unavailable(UNAVAILABLE);
            return;
        }
        try {
            portalUrl = billingClient.get().portalUrl(email);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            unavailable(NO_SUBSCRIPTION);
        } catch (Exception ex) {
            log.warn("Portal link failed for {}: {}", email, ex.getMessage());
            unavailable(UNAVAILABLE);
        }
    }

    private void unavailable(String message) {
        portalUrl = null;
        status.setText(message);
        countdownLine.setVisible(false);
        goNowButton.setVisible(false);
    }

    void goNow() {
        if (portalUrl != null) {
            getUI().ifPresent(ui -> ui.getPage().setLocation(portalUrl));
        }
    }

    /** Back to where billing lives in the app: the Profile's Billing tab. */
    void stay() {
        getUI().ifPresent(ui -> ui.navigate("profile",
            QueryParameters.simple(Map.of(ProfileView.TAB_PARAM, ProfileView.BILLING_TAB))));
    }
}
