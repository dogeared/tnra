package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

/**
 * Membership / payment screen. The entitlement gate forwards non-entitled members here (it's one of
 * the few routes a suspended member may reach). Members self-pay via the hosted Lemon Squeezy
 * checkout, or update their card via the hosted Customer Portal. No card data ever touches this app.
 *
 * <p><b>Gift mode.</b> A {@code ?gift=<email>} query param (set by the Profile "Gift" tab) makes this a
 * gift checkout: the authenticated member pays, the named member is the beneficiary. The PAYER is always
 * the authenticated user (never trusted from the client); only the beneficiary comes from the param, and
 * it's validated to be a real group member. Gifting requires the payer to be entitled themselves.
 *
 * Reachable only when {@code tnra.billing.enabled=true}; if billing is off the client is absent and
 * the view shows a simple "not enabled" message (it should never be linked in that case).
 */
@Route(value = "billing", layout = MainLayout.class)
@PermitAll
@PageTitle("Membership | TNRA")
public class BillingView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(BillingView.class);
    /** Billing status string for dunning — a member here is still giftable (rescue). */
    private static final String GRACE_PERIOD = "ON_GRACE_PERIOD";

    private final transient Optional<BillingClient> billingClient;
    private final transient UserService userService;
    private final String email;
    private final String baseUrl;

    /** Who the subscription is for: the current member (self-pay) or, in gift mode, another member. */
    private String beneficiaryEmail;
    boolean giftMode; // package-private for testing

    H2 title;               // package-private for testing
    Paragraph blurb;
    Button monthlyButton;
    Button yearlyButton;
    Paragraph manageBlurb;
    Button updatePaymentButton;
    VerticalLayout coveringSection; // "Members you're covering"; visible only when paying for others

    public BillingView(Optional<BillingClient> billingClient, OidcUserService oidcUserService,
                       UserService userService,
                       @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl) {
        this.billingClient = billingClient;
        this.userService = userService;
        this.email = oidcUserService.isAuthenticated() ? oidcUserService.getEmail() : null;
        this.beneficiaryEmail = email; // self-pay by default; beforeEnter may switch to gift
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        setAlignItems(Alignment.CENTER);

        if (billingClient.isEmpty()) {
            add(new H2("Membership"), new Paragraph("Billing is not enabled for this group."));
            return;
        }

        title = new H2("Your membership");
        blurb = new Paragraph(
            "Activate your membership to access posts, stats, and the call chain. "
            + "Choose monthly or yearly — you can change or cancel anytime.");

        monthlyButton = new Button("Pay $7 / month", e -> startCheckout("monthly"));
        monthlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        yearlyButton = new Button("Pay $60 / year", e -> startCheckout("yearly"));
        yearlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout payButtons = new HorizontalLayout(monthlyButton, yearlyButton);

        updatePaymentButton = new Button("Update payment method", e -> openPortal());
        manageBlurb = new Paragraph("Already subscribed? Update your card or manage your subscription:");

        coveringSection = buildCoveringSection();

        add(title, blurb, payButtons, manageBlurb, updatePaymentButton, coveringSection);
    }

    /**
     * Lists the members the current member is paying for (gifts), with status. Hidden when they're not
     * covering anyone. Cancelling/managing a specific gift happens in the hosted Lemon Squeezy portal
     * (reached via "Update payment method"), which lists every subscription this payer pays for.
     */
    private VerticalLayout buildCoveringSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setVisible(false);
        if (email == null) {
            return section;
        }
        try {
            List<BillingClient.CoveredMember> covered = billingClient.get().covering(email);
            if (covered.isEmpty()) {
                return section;
            }
            section.add(new H3("Members you're covering"));
            for (BillingClient.CoveredMember m : covered) {
                section.add(new Paragraph(m.email() + " — " + friendlyStatus(m.status())));
            }
            section.add(new Paragraph(
                "To change a card or stop covering someone, use \"Update payment method\" above — "
                + "the payment portal lists every subscription you pay for."));
            section.setVisible(true);
        } catch (Exception e) {
            // Non-critical: if the covering list can't load, just don't show it.
            log.warn("Couldn't load covering list for {}: {}", email, e.getMessage());
        }
        return section;
    }

    private static String friendlyStatus(String status) {
        return status == null ? "" : status.toLowerCase().replace('_', ' ');
    }

    /** Switch into gift mode when a valid {@code ?gift=<email>} is present; otherwise stay self-pay. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (billingClient.isEmpty() || email == null) {
            return;
        }
        List<String> gift = event.getLocation().getQueryParameters()
            .getParameters().getOrDefault("gift", List.of());
        if (!gift.isEmpty() && !gift.get(0).isBlank()) {
            applyGiftMode(gift.get(0).trim().toLowerCase());
        }
    }

    private void applyGiftMode(String giftEmail) {
        if (giftEmail.equalsIgnoreCase(email)) {
            return; // gifting to yourself is just self-pay — ignore the param
        }
        // Only an entitled member may gift one to someone else.
        if (!billingClient.get().isEntitled(email)) {
            Notification.show("Activate your own membership before gifting one to someone else.");
            return;
        }
        User recipient = userService.getUserByEmail(giftEmail);
        if (recipient == null) {
            Notification.show("That member couldn't be found. Showing your own membership options.");
            return;
        }
        // Already SETTLED? Don't let a redundant gift through (the billing service enforces this too with
        // a 409). A member in dunning (ON_GRACE_PERIOD — failing their own payment) is intentionally
        // still giftable as a rescue, so only block when entitled AND not in grace.
        BillingClient.Entitlement ben = billingClient.get().entitlement(giftEmail);
        if (ben.entitled() && !GRACE_PERIOD.equals(ben.status())) {
            title.setText("Gift a membership");
            blurb.setText(displayName(recipient) + " (" + giftEmail + ") already has an active "
                + "membership — no gift needed.");
            monthlyButton.setEnabled(false);
            yearlyButton.setEnabled(false);
            manageBlurb.setVisible(false);
            updatePaymentButton.setVisible(false);
            coveringSection.setVisible(false);
            return;
        }
        giftMode = true;
        beneficiaryEmail = giftEmail;
        title.setText("Gift a membership");
        blurb.setText("You're paying for " + displayName(recipient) + " (" + giftEmail + "). "
            + "Choose monthly or yearly — their membership activates once payment completes.");
        // The manage/portal and "you're covering" sections are about the payer's own state, not this gift.
        manageBlurb.setVisible(false);
        updatePaymentButton.setVisible(false);
        coveringSection.setVisible(false);
    }

    void startCheckout(String variant) {
        if (email == null || billingClient.isEmpty()) {
            return;
        }
        try {
            // Self-pay returns to the "activating" page (the payer's own entitlement is what flips).
            // A gift returns to the app home — the gifter is already active, only the beneficiary changes.
            String redirectUrl = giftMode ? baseUrl : baseUrl + "/billing/activating";
            String url = billingClient.get().createCheckout(beneficiaryEmail, variant, email, redirectUrl);
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (HttpClientErrorException.Conflict ex) {
            Notification.show(giftMode
                ? "That member already has an active subscription."
                : "You're already a member. Use \"Update payment method\" to manage your subscription.");
        } catch (Exception ex) {
            log.warn("Checkout failed (beneficiary={}, variant={}): {}", beneficiaryEmail, variant, ex.getMessage());
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

    private static String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }
}
