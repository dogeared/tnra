package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.model.RequestAccess;
import com.afitnerd.tnra.landing.service.RequestAccessService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.dependency.CssImport;

@Route("")
@PageTitle("TNRA — Accountability for Groups That Mean It")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class LandingView extends VerticalLayout {

    private final RequestAccessService requestAccessService;

    public LandingView(RequestAccessService requestAccessService) {
        this.requestAccessService = requestAccessService;
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(buildNav(), buildHero(), buildFeatures(), buildForm(), buildFooter());
    }

    private Component buildNav() {
        HorizontalLayout nav = new HorizontalLayout();
        nav.addClassName("landing-nav");
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span logo = new Span("TNRA");
        logo.addClassName("nav-logo");

        Anchor requestLink = new Anchor("#request-access", "Request Access");
        requestLink.addClassName("nav-link");

        nav.add(logo, requestLink);
        return nav;
    }

    private Component buildHero() {
        Div hero = new Div();
        hero.addClassName("landing-hero");

        H1 headline = new H1("Accountability for groups that mean it.");
        headline.addClassName("hero-headline");

        Paragraph sub = new Paragraph(
            "TNRA gives recovery groups, faith communities, and professional accountability circles " +
            "a structured home for weekly posts, group stats, and mutual visibility — " +
            "with end-to-end encryption so your content stays yours."
        );
        sub.addClassName("hero-sub");

        Anchor cta = new Anchor("#request-access", "Request Access for Your Group");
        cta.addClassName("hero-cta");

        hero.add(headline, sub, cta);
        return hero;
    }

    private Component buildFeatures() {
        Div features = new Div();
        features.addClassName("features-section");

        H2 title = new H2("What your group gets");
        title.addClassName("features-title");

        Div grid = new Div();
        grid.addClassName("features-grid");
        grid.add(
            featureCard("📝", "Structured weekly posts",
                "Every member fills out the same format: intro, kryptonite, commitments, best & worst across life domains, and configurable stats. Consistency builds accountability."),
            featureCard("🔔", "Real-time Slack notifications",
                "When a member finishes a post, your group's Slack channel gets a notification with a secure deep link. No more wondering who posted."),
            featureCard("🔒", "Encrypted at rest",
                "All post content and stats are encrypted with AES-256-GCM. Your group's entries don't leave the encrypted database unless you choose to share them.")
        );

        features.add(title, grid);
        return features;
    }

    private Component featureCard(String icon, String heading, String body) {
        Div card = new Div();
        card.addClassName("feature-card");

        Span iconEl = new Span(icon);
        iconEl.addClassName("feature-icon");

        H3 h = new H3(heading);
        h.addClassName("feature-heading");

        Paragraph p = new Paragraph(body);
        p.addClassName("feature-body");

        card.add(iconEl, h, p);
        return card;
    }

    private Component buildForm() {
        Div section = new Div();
        section.addClassName("form-section");
        section.setId("request-access");

        Div card = new Div();
        card.addClassName("form-card");

        H2 title = new H2("Request access");
        title.addClassName("form-title");

        Paragraph intro = new Paragraph(
            "TNRA is invite-only. Tell us about your group and we'll be in touch."
        );
        intro.addClassName("form-intro");

        TextField groupName = new TextField("Group name");
        groupName.setRequired(true);
        groupName.setWidthFull();
        groupName.addClassName("form-field");

        TextField contactName = new TextField("Your name");
        contactName.setRequired(true);
        contactName.setWidthFull();
        contactName.addClassName("form-field");

        EmailField email = new EmailField("Your email");
        email.setRequired(true);
        email.setWidthFull();
        email.addClassName("form-field");

        IntegerField estimatedSize = new IntegerField("Estimated group size");
        estimatedSize.setMin(1);
        estimatedSize.setWidthFull();
        estimatedSize.addClassName("form-field");

        TextArea description = new TextArea("Tell us about your group");
        description.setPlaceholder("What brings your group together? What are you hoping TNRA will help with?");
        description.setWidthFull();
        description.setMinHeight("120px");
        description.addClassName("form-field");

        Button submit = new Button("Send Request");
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submit.addClassName("submit-button");

        Div successMsg = new Div();
        successMsg.addClassName("success-message");
        successMsg.setVisible(false);
        successMsg.add(new Span("Thanks! We'll be in touch at "), new Span(), new Span("."));

        submit.addClickListener(e -> {
            if (!validateForm(groupName, contactName, email)) return;

            String ipAddress = extractIpAddress();
            if (requestAccessService.isRateLimited(ipAddress)) {
                showError("Too many requests from your network. Please try again later.");
                return;
            }

            RequestAccess request = new RequestAccess();
            request.setGroupName(groupName.getValue().trim());
            request.setContactName(contactName.getValue().trim());
            request.setEmail(email.getValue().trim());
            request.setEstimatedSize(estimatedSize.getValue());
            request.setDescription(description.getValue());
            request.setIpAddress(ipAddress);

            requestAccessService.submit(request);

            groupName.setEnabled(false);
            contactName.setEnabled(false);
            email.setEnabled(false);
            estimatedSize.setEnabled(false);
            description.setEnabled(false);
            submit.setEnabled(false);
            submit.setText("Sent!");

            Span emailSpan = new Span(request.getEmail());
            emailSpan.getStyle().set("font-weight", "600");
            successMsg.removeAll();
            successMsg.add(new Text("Thanks! We'll be in touch at "), emailSpan, new Text("."));
            successMsg.setVisible(true);
        });

        card.add(title, intro, groupName, contactName, email, estimatedSize, description, submit, successMsg);
        section.add(card);
        return section;
    }

    private Component buildFooter() {
        Footer footer = new Footer();
        footer.addClassName("landing-footer");
        footer.add(new Paragraph("© 2026 TNRA. Built for groups that take commitment seriously."));
        return footer;
    }

    private boolean validateForm(TextField groupName, TextField contactName, EmailField email) {
        boolean valid = true;
        if (groupName.getValue().isBlank()) {
            groupName.setInvalid(true);
            groupName.setErrorMessage("Group name is required");
            valid = false;
        }
        if (contactName.getValue().isBlank()) {
            contactName.setInvalid(true);
            contactName.setErrorMessage("Your name is required");
            valid = false;
        }
        if (email.getValue().isBlank() || email.isInvalid()) {
            email.setInvalid(true);
            email.setErrorMessage("A valid email is required");
            valid = false;
        }
        return valid;
    }

    private String extractIpAddress() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
