package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.content.LandingContentParser;
import com.afitnerd.tnra.landing.content.LandingContentParser.Block;
import com.afitnerd.tnra.landing.content.LandingContentRenderer;
import com.afitnerd.tnra.landing.model.RequestAccess;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.afitnerd.tnra.landing.service.RequestAccessService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Map;

@Route("")
@PageTitle("TNRA — Accountability for Groups That Mean It")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class LandingView extends VerticalLayout {

    static final String CONTENT_RESOURCE = "/content/landing.md";

    private final RequestAccessService requestAccessService;

    public LandingView(RequestAccessService requestAccessService,
                       MarkdownService markdownService,
                       LandingContentRenderer renderer) {
        this.requestAccessService = requestAccessService;
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(LandingChrome.nav(true));
        renderer.render(
            LandingContentParser.parse(markdownService.readClasspathResource(CONTENT_RESOURCE)),
            this::buildCustomBlock
        ).forEach(this::add);
        add(LandingChrome.footer());
    }

    private Component buildCustomBlock(Block block) {
        return "form".equals(block.type()) ? buildForm(block) : null;
    }

    private Component buildForm(Block block) {
        Map<String, String> fields = LandingContentParser.parseFields(block.body());
        String successTemplate = fields.getOrDefault("success", "Thanks! We'll be in touch at {email}.");
        String groupLabel = fields.getOrDefault("group-label", "Group name");
        String nameLabel = fields.getOrDefault("name-label", "Your name");
        String emailLabel = fields.getOrDefault("email-label", "Your email");

        Div section = new Div();
        section.addClassName("form-section");
        section.setId("request-access");

        Div card = new Div();
        card.addClassName("form-card");

        H2 title = new H2(fields.getOrDefault("title", "Request access"));
        title.addClassName("form-title");

        Paragraph intro = new Paragraph(
            fields.getOrDefault("intro", "TNRA is invite-only. Tell us about your group and we'll be in touch.")
        );
        intro.addClassName("form-intro");

        TextField groupName = new TextField(groupLabel);
        groupName.setRequired(true);
        groupName.setErrorMessage(groupLabel + " is required");
        groupName.setWidthFull();
        groupName.addClassName("form-field");

        TextField contactName = new TextField(nameLabel);
        contactName.setRequired(true);
        contactName.setErrorMessage(nameLabel + " is required");
        contactName.setWidthFull();
        contactName.addClassName("form-field");

        EmailField email = new EmailField(emailLabel);
        email.setRequired(true);
        email.setErrorMessage("A valid email is required");
        email.setWidthFull();
        email.addClassName("form-field");

        IntegerField estimatedSize = new IntegerField(fields.getOrDefault("size-label", "Estimated group size"));
        estimatedSize.setMin(1);
        estimatedSize.setWidthFull();
        estimatedSize.addClassName("form-field");

        TextArea description = new TextArea(fields.getOrDefault("description-label", "Tell us about your group"));
        description.setPlaceholder(fields.getOrDefault("description-placeholder",
            "What brings your group together? What are you hoping TNRA will help with?"));
        description.setWidthFull();
        description.setMinHeight("120px");
        description.addClassName("form-field");

        Button submit = new Button(fields.getOrDefault("submit", "Send Request"));
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submit.addClassName("submit-button");
        String sentLabel = fields.getOrDefault("sent", "Sent!");

        Div successMsg = new Div();
        successMsg.addClassName("success-message");
        successMsg.setVisible(false);

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
            submit.setText(sentLabel);

            showSuccess(successMsg, successTemplate, request.getEmail());
        });

        card.add(title, intro, groupName, contactName, email, estimatedSize, description, submit, successMsg);
        section.add(card);
        return section;
    }

    /** Render the success template, substituting a bolded {email}. */
    private void showSuccess(Div successMsg, String template, String emailValue) {
        Span emailSpan = new Span(emailValue);
        emailSpan.getStyle().set("font-weight", "600");

        String[] parts = template.split("\\{email\\}", 2);
        successMsg.removeAll();
        successMsg.add(new Text(parts[0]), emailSpan);
        if (parts.length > 1) {
            successMsg.add(new Text(parts[1]));
        }
        successMsg.setVisible(true);
    }

    private boolean validateForm(TextField groupName, TextField contactName, EmailField email) {
        // Error messages are set on each field at creation (derived from the
        // content-supplied labels); validation just toggles the invalid state.
        boolean valid = true;
        if (groupName.getValue().isBlank()) {
            groupName.setInvalid(true);
            valid = false;
        }
        if (contactName.getValue().isBlank()) {
            contactName.setInvalid(true);
            valid = false;
        }
        if (email.getValue().isBlank() || email.isInvalid()) {
            email.setInvalid(true);
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
