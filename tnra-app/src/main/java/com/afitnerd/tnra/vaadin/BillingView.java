package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Membership / payment screen. The entitlement gate forwards non-entitled members here (it's one of
 * the few routes a suspended member may reach). Members self-pay via the hosted Lemon Squeezy
 * checkout, or update their card via the hosted Customer Portal. No card data ever touches this app.
 *
 * Reachable only when {@code tnra.billing.enabled=true}; if billing is off the client is absent and
 * the view shows a simple "not enabled" message (it should never be linked in that case).
 */
@Route(value = "billing", layout = MainLayout.class)
@PermitAll
@PageTitle("Membership | TNRA")
public class BillingView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(BillingView.class);

    private final transient Optional<BillingClient> billingClient;
    private final String email;

    Button monthlyButton;   // package-private for testing
    Button yearlyButton;
    Button updatePaymentButton;

    public BillingView(Optional<BillingClient> billingClient, OidcUserService oidcUserService) {
        this.billingClient = billingClient;
        this.email = oidcUserService.isAuthenticated() ? oidcUserService.getEmail() : null;

        setAlignItems(Alignment.CENTER);

        if (billingClient.isEmpty()) {
            add(new H2("Membership"), new Paragraph("Billing is not enabled for this group."));
            return;
        }

        H2 title = new H2("Your membership");
        Paragraph blurb = new Paragraph(
            "Activate your membership to access posts, stats, and the call chain. "
            + "Choose monthly or yearly — you can change or cancel anytime.");

        monthlyButton = new Button("Pay $7 / month", e -> startCheckout("monthly"));
        monthlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        yearlyButton = new Button("Pay $60 / year", e -> startCheckout("yearly"));
        yearlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout payButtons = new HorizontalLayout(monthlyButton, yearlyButton);

        updatePaymentButton = new Button("Update payment method", e -> openPortal());

        Paragraph manageBlurb = new Paragraph(
            "Already subscribed? Update your card or manage your subscription:");

        add(title, blurb, payButtons, manageBlurb, updatePaymentButton);
    }

    void startCheckout(String variant) {
        if (email == null || billingClient.isEmpty()) {
            return;
        }
        try {
            String url = billingClient.get().createCheckout(email, variant, email);
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (Exception ex) {
            log.warn("Checkout failed for {} ({}): {}", email, variant, ex.getMessage());
            Notification.show("Sorry, we couldn't start checkout. Please try again.");
        }
    }

    void openPortal() {
        if (email == null || billingClient.isEmpty()) {
            return;
        }
        try {
            String url = billingClient.get().portalUrl(email);
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (Exception ex) {
            log.warn("Portal link failed for {}: {}", email, ex.getMessage());
            Notification.show("Sorry, we couldn't open the payment portal. Please try again.");
        }
    }
}
