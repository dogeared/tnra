package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One-line warning shown at the top of EVERY page (via {@link MainLayout}) for a member whose last
 * payment failed ({@code ON_GRACE_PERIOD}). Such a member is still entitled and roams the whole app, so a
 * prompt on the home or profile page would be easy to never see — this is the surface that actually
 * reaches them. Hidden in every other state; fails closed to hidden on a billing error.
 */
public class MembershipBanner extends HorizontalLayout {

    private static final Logger log = LoggerFactory.getLogger(MembershipBanner.class);

    static final String GRACE_TEXT =
        "Your last membership payment didn't go through. Update your payment method to keep your access.";

    private transient BillingClient billingClient;
    private String email;
    private final String baseUrl;

    final Span text = new Span(GRACE_TEXT);
    final Button updatePaymentButton = new Button("Update payment method", e -> startCardUpdate());

    public MembershipBanner(String baseUrl) {
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        addClassName("membership-banner");
        setWidthFull();
        setPadding(true);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        getStyle()
            .set("background", "var(--lumo-error-color-10pct)")
            .set("border-bottom", "1px solid var(--lumo-error-color-50pct)")
            .set("box-sizing", "border-box")
            .set("flex-wrap", "wrap")
            .set("gap", "var(--lumo-space-m)");
        updatePaymentButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        add(text, updatePaymentButton);
        setVisible(false);
    }

    /**
     * Re-evaluate for the given member.
     * @return true when the banner is shown (member is in dunning).
     */
    public boolean refresh(BillingClient billingClient, String email) {
        this.billingClient = billingClient;
        this.email = email;
        if (billingClient == null || email == null) {
            setVisible(false);
            return false;
        }
        try {
            BillingClient.Entitlement ent = billingClient.entitlement(email);
            boolean grace = ent != null && "ON_GRACE_PERIOD".equals(ent.status());
            setVisible(grace);
            return grace;
        } catch (Exception e) {
            log.warn("Couldn't load entitlement for {}: {}", email, e.getMessage());
            setVisible(false);
            return false;
        }
    }

    /** Same overlay as a purchase: completing it stores the new card and settles the failed charge. */
    void startCardUpdate() {
        if (billingClient == null || email == null) {
            return;
        }
        try {
            String url = billingClient.paymentMethodUpdateUrl(email);
            String done = baseUrl + "/billing/activating";
            getUI().ifPresent(ui -> ui.getPage().setLocation(BillingView.withReturnUrls(url, done, baseUrl)));
        } catch (Exception ex) {
            log.warn("Card update link failed for {}: {}", email, ex.getMessage());
            AppNotification.error("Sorry, we couldn't start the card update. Please try again.");
        }
    }
}
