package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;

/**
 * Post-checkout landing page. Lemon Squeezy returns the buyer here (the checkout's redirect_url) right
 * after a successful payment, but entitlement only flips to ACTIVE when the webhook lands a moment later.
 * Rather than bounce the member back to the membership page in that gap, this page polls the billing
 * service (cache-bypassing) and forwards them into the app the instant the webhook is processed.
 *
 * <p>Security: access is granted only on the REAL webhook flipping the account ACTIVE — this page never
 * grants access itself, it just waits. It's in {@link EntitlementGate}'s allow-list so a not-yet-active
 * member isn't bounced off it mid-wait.
 */
@Route(value = "billing/activating", layout = MainLayout.class)
@PermitAll
@PageTitle("Activating | TNRA")
public class BillingActivatingView extends VerticalLayout {

    static final int POLL_INTERVAL_MS = 2000;
    static final int MAX_POLLS = 30; // ~60s; webhooks normally land in a few seconds

    private final transient Optional<BillingClient> billingClient;
    private final String email;

    int polls; // package-private for testing
    private final Paragraph status = new Paragraph(
        "Hang tight — we're confirming your payment. This usually takes just a few seconds.");
    Button continueButton; // package-private for testing; shown only on timeout

    public BillingActivatingView(Optional<BillingClient> billingClient, OidcUserService oidcUserService) {
        this.billingClient = billingClient;
        this.email = oidcUserService.isAuthenticated() ? oidcUserService.getEmail() : null;

        setAlignItems(Alignment.CENTER);
        ProgressBar spinner = new ProgressBar();
        spinner.setIndeterminate(true);
        add(new H2("Activating your membership…"), status, spinner);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (!shouldPoll()) {
            forwardToApp(); // billing off or not authenticated — nothing to wait for
            return;
        }
        UI ui = attachEvent.getUI();
        ui.setPollInterval(POLL_INTERVAL_MS);
        ui.addPollListener(e -> tick());
        tick(); // check immediately in case the webhook already landed
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        detachEvent.getUI().setPollInterval(-1); // stop polling when we leave
    }

    boolean shouldPoll() {
        return billingClient.isPresent() && email != null;
    }

    /** One poll cycle. Returns true once polling should stop (member is in, or we gave up waiting). */
    boolean tick() {
        polls++;
        if (entitledNow()) {
            stopPolling();
            forwardToApp();
            return true;
        }
        if (polls >= MAX_POLLS) {
            stopPolling();
            showTimeout();
            return true;
        }
        return false;
    }

    boolean entitledNow() {
        return billingClient.isEmpty() || email == null || billingClient.get().isEntitledFresh(email);
    }

    private void stopPolling() {
        getUI().ifPresent(ui -> ui.setPollInterval(-1));
    }

    private void forwardToApp() {
        getUI().ifPresent(ui -> ui.navigate(MainView.class));
    }

    private void showTimeout() {
        status.setText("This is taking a little longer than usual — your payment is still processing. "
            + "Continue to TNRA and your membership should be active shortly.");
        if (continueButton == null) {
            continueButton = new Button("Continue to TNRA", e -> forwardToApp());
            add(continueButton);
        }
    }
}
