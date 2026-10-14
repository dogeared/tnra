package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * "Powered by Paddle" — a small link shown wherever a member can pay, opening a short explanation of
 * who Paddle is and, above all, that the emails about their membership (receipts, cancellations, failed
 * payments) will come from Paddle, not from TNRA. Shown only where payment actually applies; a covered
 * (exempt) group never sees it.
 */
public class PaddleInfoLink extends HorizontalLayout {

    static final String LINK_TEXT = "Powered by Paddle";
    static final String TITLE = "About Paddle";
    static final String PADDLE_URL = "https://www.paddle.com";

    static final String INTRO =
        "TNRA memberships are sold through Paddle, our payment partner and Merchant of Record. Paddle "
        + "handles the checkout, your card details, receipts and any applicable taxes — TNRA never sees or "
        + "stores your card.";
    static final String EMAILS_LEAD = "Because of that, some emails about your membership come from Paddle rather than TNRA:";
    static final String[] EMAILS = {
        "a receipt when you pay or when a renewal goes through,",
        "a confirmation when you cancel a membership,",
        "a notice if a payment fails, so you can update your card."
    };
    static final String OUTRO = "They're genuine. If one ever looks off, check it against your Billing tab here, "
        + "where the same information is shown.";

    final Button link = new Button(LINK_TEXT, e -> openDialog());
    Dialog dialog; // package-private for testing; created on first open

    public PaddleInfoLink() {
        addClassName("paddle-info");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        link.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        link.getStyle().set("color", "var(--lumo-secondary-text-color)");
        add(link);
    }

    void openDialog() {
        if (dialog == null) {
            dialog = buildDialog();
        }
        dialog.open();
    }

    Dialog buildDialog() {
        Dialog d = new Dialog();
        d.setHeaderTitle(TITLE);
        d.setMaxWidth("36rem");
        UnorderedList emails = new UnorderedList();
        for (String line : EMAILS) {
            emails.add(new ListItem(line));
        }
        Anchor site = new Anchor(PADDLE_URL, "paddle.com");
        site.setTarget("_blank");
        Span learnMore = new Span();
        learnMore.add(new Span("Learn more at "), site, new Span("."));
        d.add(new Paragraph(INTRO), new Paragraph(EMAILS_LEAD), emails, new Paragraph(OUTRO), learnMore);
        Button close = new Button("Got it", e -> d.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(close);
        return d;
    }
}
