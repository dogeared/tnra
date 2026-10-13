package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Compact "your membership" panel for the pages a not-yet-paying member may still reach (the home
 * welcome and the bottom of every Profile tab — see {@link EntitlementGate}'s allow-list). It is the same
 * surface as the {@code /billing} page, minus gifting: a status line and the pay / update-card actions.
 *
 * <p>Rendered only when there is something to do: hidden for an entitled member (including one in
 * ON_GRACE_PERIOD — that warning is {@link MembershipBanner}'s job, on every page), shown as a
 * call-to-action for PENDING / SUSPENDED. Fails closed to <i>hidden</i>: a billing hiccup must never
 * clutter a page with a bogus "pay now".
 */
public class MembershipPrompt extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(MembershipPrompt.class);

    static final String PENDING_TEXT =
        "Activate your membership to access posts, stats, and the call chain. "
        + "Choose monthly or yearly — you can change or cancel anytime.";
    static final String SUSPENDED_TEXT =
        "Your membership is suspended. Choose monthly or yearly to reactivate it.";

    private final transient BillingClient billingClient;
    private final String email;
    private final String baseUrl;

    final H3 title = new H3("Your membership");
    final Paragraph blurb = new Paragraph(PENDING_TEXT);
    final Button monthlyButton = new Button("Pay $7 / month", e -> startCheckout("monthly"));
    final Button yearlyButton = new Button("Pay $60 / year", e -> startCheckout("yearly"));
    final HorizontalLayout payButtons = new HorizontalLayout(monthlyButton, yearlyButton);
    final Button updatePaymentButton = new Button("Update payment method", e -> openPortal());

    public MembershipPrompt(BillingClient billingClient, String email, String baseUrl) {
        this.billingClient = billingClient;
        this.email = email;
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        addClassName("membership-prompt");
        setPadding(false);
        setSpacing(true);
        monthlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        yearlyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(new Hr(), title, blurb, payButtons, updatePaymentButton);
        setVisible(false); // until refresh() says there's something to do
    }

    /** Centre everything — for the home page, where it sits under a centred welcome. */
    public MembershipPrompt centered() {
        setAlignItems(Alignment.CENTER);
        blurb.getStyle().set("text-align", "center");
        blurb.setMaxWidth("40rem");
        return this;
    }

    /**
     * Re-read entitlement and show/hide accordingly.
     * @return true when the prompt is shown (member has something to do about their membership).
     */
    public boolean refresh() {
        if (email == null) {
            setVisible(false);
            return false;
        }
        BillingClient.Entitlement ent;
        try {
            ent = billingClient.entitlement(email);
        } catch (Exception e) {
            log.warn("Couldn't load entitlement for {}: {}", email, e.getMessage());
            setVisible(false);
            return false;
        }
        if (ent == null) {
            setVisible(false);
            return false;
        }
        String status = ent.status();
        if (ent.entitled()) {
            // Includes ON_GRACE_PERIOD: still in, and the "card failing" warning is the banner's job.

            setVisible(false);
            return false;
        }
        boolean suspended = "SUSPENDED".equals(status);
        blurb.setText(suspended ? SUSPENDED_TEXT : PENDING_TEXT);
        payButtons.setVisible(true);
        // No subscription yet ⇒ nothing to update; suspended ⇒ the portal can reactivate.
        updatePaymentButton.setVisible(suspended);
        setVisible(true);
        return true;
    }

    void startCheckout(String variant) {
        if (email == null) {
            return;
        }
        try {
            String done = baseUrl + "/billing/activating";
            String url = billingClient.createCheckout(email, variant, email, done);
            getUI().ifPresent(ui -> ui.getPage().setLocation(
                BillingView.withReturnUrls(url, done, baseUrl + "/billing")));
        } catch (HttpClientErrorException.Conflict ex) {
            AppNotification.error(
                "You're already a member. Use \"Update payment method\" to manage your subscription.");
        } catch (Exception ex) {
            log.warn("Checkout failed for {} ({}): {}", email, variant, ex.getMessage());
            AppNotification.error("Sorry, we couldn't start checkout. Please try again.");
        }
    }

    void openPortal() {
        if (email == null) {
            return;
        }
        try {
            String url = billingClient.portalUrl(email);
            getUI().ifPresent(ui -> ui.getPage().setLocation(url));
        } catch (Exception ex) {
            log.warn("Portal link failed for {}: {}", email, ex.getMessage());
            AppNotification.error("Sorry, we couldn't open the payment portal. Please try again.");
        }
    }
}
