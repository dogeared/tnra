package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Membership / payment screen. The entitlement gate forwards non-entitled members here (it's one of
 * the few routes a suspended member may reach). Members self-pay via the hosted Paddle
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
    /**
     * "Cover everyone" chain: the members still to gift AFTER the current one. Paddle makes each gift its
     * own subscription (so any one can be stopped later), hence one checkout per member; a completed
     * checkout returns straight to the next member's, and the last one to the Profile Billing tab.
     */
    List<String> giftQueue = List.of(); // package-private for testing

    static final String GIFT_PARAM = "gift";
    static final String QUEUE_PARAM = "queue";

    H2 title;               // package-private for testing
    Paragraph blurb;
    Button monthlyButton;
    Button yearlyButton;
    Paragraph manageBlurb;
    Button updatePaymentButton;
    Button manageButton;
    Button backToProfileButton; // always present: this page must never be a dead end
    PaddleInfoLink paddleInfo;
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

        updatePaymentButton = new Button("Update payment method", e -> startCardUpdate());
        manageButton = new Button("Invoices & receipts", e -> openPortal());
        manageBlurb = new Paragraph("Already subscribed? Update your card right here; invoices and receipts "
            + "are in Paddle's portal. Cancel anytime from your Profile → Billing tab.");
        HorizontalLayout manageButtons = new HorizontalLayout(updatePaymentButton, manageButton);

        coveringSection = buildCoveringSection();

        backToProfileButton = new Button("Back to profile", VaadinIcon.ARROW_LEFT.create(), e -> backToProfile());
        backToProfileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        paddleInfo = new PaddleInfoLink();

        add(title, blurb, payButtons, manageBlurb, manageButtons, coveringSection, paddleInfo, backToProfileButton);
    }

    /**
     * Lists the members the current member is paying for (gifts), with status. Hidden when they're not
     * covering anyone. Cancelling/managing a specific gift happens in the hosted Paddle portal
     * (reached via "Update payment method"), which lists every subscription this payer pays for.
     */
    private VerticalLayout buildCoveringSection() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setAlignItems(Alignment.CENTER);
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
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        List<String> gift = params.getOrDefault(GIFT_PARAM, List.of());
        boolean giftRequested = !gift.isEmpty() && !gift.get(0).isBlank();
        if (giftRequested) {
            List<String> queue = params.getOrDefault(QUEUE_PARAM, List.of());
            giftQueue = parseQueue(queue.isEmpty() ? null : queue.get(0));
            applyGiftMode(gift.get(0).trim().toLowerCase(), event);
            return;
        }
        // This page is the paywall: "pay to get in". A member who IS in has nothing to buy here — their
        // membership (status, card, cancel, gifts) lives on the Profile's Billing tab. Showing them pay
        // buttons was wrong and confusing.
        if (billingClient.get().isEntitled(email)) {
            event.forwardTo("profile", QueryParameters.simple(Map.of(ProfileView.TAB_PARAM, ProfileView.BILLING_TAB)));
        }
    }

    private void applyGiftMode(String giftEmail, BeforeEnterEvent event) {
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
            if (!giftQueue.isEmpty()) {
                // Mid-chain and this member got covered in the meantime (someone else gifted them, or a
                // trial kicked in): skip straight to the next one rather than dead-ending the chain.
                event.forwardTo("billing", giftParams(giftQueue.get(0), giftQueue.subList(1, giftQueue.size())));
                return;
            }
            title.setText("Gift a membership");
            blurb.setText(displayName(recipient) + " (" + giftEmail + ") already has an active "
                + "membership — no gift needed.");
            monthlyButton.setEnabled(false);
            yearlyButton.setEnabled(false);
            manageBlurb.setVisible(false);
            updatePaymentButton.setVisible(false);
        manageButton.setVisible(false);
            manageButton.setVisible(false);
            coveringSection.setVisible(false);
            return;
        }
        giftMode = true;
        beneficiaryEmail = giftEmail;
        title.setText("Gift a membership");
        blurb.setText("You're paying for " + displayName(recipient) + " (" + giftEmail + "). "
            + "Choose monthly or yearly — their membership activates once payment completes."
            + (giftQueue.isEmpty() ? "" : " " + giftQueue.size() + " more to go after this one — each "
                + "completed checkout continues to the next member automatically."));
        // The manage/portal and "you're covering" sections are about the payer's own state, not this gift.
        manageBlurb.setVisible(false);
        updatePaymentButton.setVisible(false);
        manageButton.setVisible(false);
        coveringSection.setVisible(false);
    }

    void startCheckout(String variant) {
        if (email == null || billingClient.isEmpty()) {
            return;
        }
        try {
            // After a COMPLETED checkout: self-pay goes to the "activating" page (the payer's own entitlement
            // is what flips); a gift returns to the Profile's Billing tab, where the gift was started and
            // where "Members you're covering" shows the result (home would bounce to Stats).
            // In a "cover everyone" chain the done URL is the NEXT member's gift checkout.
            String redirectUrl = giftMode
                ? (giftQueue.isEmpty()
                    ? baseUrl + "/profile?" + ProfileView.TAB_PARAM + "=" + ProfileView.BILLING_TAB
                    : giftUrl(baseUrl, giftQueue.get(0), giftQueue.subList(1, giftQueue.size())))
                : baseUrl + "/billing/activating";
            // If the member CLOSES the overlay without paying: back to where they started (this page, in
            // the same mode, same remaining chain) — never to the activating page, which would sit waiting
            // for a payment that never happened.
            String backUrl = giftMode
                ? giftUrl(baseUrl, beneficiaryEmail, giftQueue)
                : baseUrl + "/billing";
            String url = billingClient.get().createCheckout(beneficiaryEmail, variant, email, redirectUrl);
            getUI().ifPresent(ui -> ui.getPage().setLocation(withReturnUrls(url, redirectUrl, backUrl)));
        } catch (HttpClientErrorException.Conflict ex) {
            Notification.show(giftMode
                ? "That member already has an active subscription."
                : "You're already a member. Use \"Update payment method\" to manage your subscription.");
        } catch (Exception ex) {
            log.warn("Checkout failed (beneficiary={}, variant={}): {}", beneficiaryEmail, variant, ex.getMessage());
            Notification.show("Sorry, we couldn't start checkout. Please try again.");
        }
    }

    /**
     * The checkout URL Paddle hands back is the landing site's {@code /pay} page plus {@code _ptxn}. Append
     * where to send the member afterwards — {@code done} after a completed checkout, {@code return} if they
     * close the overlay unpaid — so that page knows the way back to THIS group (every group has its own
     * subdomain) from the first paint, instead of depending on Paddle echoing custom data.
     */
    static String withReturnUrls(String checkoutUrl, String doneUrl, String backUrl) {
        if (checkoutUrl == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(checkoutUrl);
        appendParam(sb, "done", doneUrl);
        appendParam(sb, "return", backUrl);
        return sb.toString();
    }

    /** {@code /billing?gift=<next>&queue=<rest>} as an absolute URL (Paddle's done/return targets). */
    static String giftUrl(String baseUrl, String next, List<String> rest) {
        StringBuilder sb = new StringBuilder(baseUrl).append("/billing");
        appendParam(sb, GIFT_PARAM, next);
        appendParam(sb, QUEUE_PARAM, queueParam(rest));
        return sb.toString();
    }

    /** The same target as router query parameters (in-app forwards). */
    static QueryParameters giftParams(String next, List<String> rest) {
        Map<String, String> p = new java.util.LinkedHashMap<>();
        p.put(GIFT_PARAM, next);
        if (!rest.isEmpty()) {
            p.put(QUEUE_PARAM, queueParam(rest));
        }
        return QueryParameters.simple(p);
    }

    /** Comma-joined, lower-cased emails; empty string for an empty list. */
    static String queueParam(List<String> emails) {
        return emails.stream().map(e -> e.trim().toLowerCase()).filter(e -> !e.isEmpty())
            .collect(java.util.stream.Collectors.joining(","));
    }

    static List<String> parseQueue(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(",")).map(String::trim).map(String::toLowerCase)
            .filter(e -> !e.isEmpty()).distinct().toList();
    }

    private static void appendParam(StringBuilder url, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        url.append(url.indexOf("?") >= 0 ? '&' : '?')
           .append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    /** Card update in the same /pay overlay as a purchase; returns to the app. */
    void startCardUpdate() {
        if (email == null || billingClient.isEmpty()) {
            return;
        }
        try {
            String url = billingClient.get().paymentMethodUpdateUrl(email);
            String done = baseUrl + "/billing/activating";
            getUI().ifPresent(ui -> ui.getPage().setLocation(withReturnUrls(url, done, baseUrl + "/billing")));
        } catch (HttpClientErrorException.NotFound ex) {
            Notification.show("You don't have a subscription to update yet — choose a plan above.");
        } catch (HttpClientErrorException.Conflict ex) {
            Notification.show("Your membership is a gift — the person paying for it manages the card.");
        } catch (Exception ex) {
            log.warn("Card update link failed for {}: {}", email, ex.getMessage());
            Notification.show("Sorry, we couldn't start the card update. Please try again.");
        }
    }

    /** Leave this page the way the member came: the Profile's Billing tab (gifting lives there). */
    void backToProfile() {
        getUI().ifPresent(ui -> ui.navigate("profile",
            QueryParameters.simple(Map.of(ProfileView.TAB_PARAM, ProfileView.BILLING_TAB))));
    }

    /** Invoices / cancel / pause live in Paddle's full-page portal — go via the interstitial. */
    void openPortal() {
        getUI().ifPresent(ui -> ui.navigate(BillingPortalView.class));
    }

    private static String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }
}
