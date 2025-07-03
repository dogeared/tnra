package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
public class StatsView extends VerticalLayout {

    private final PostService postService;
    private final UserService userService;
    private Post currentPost;
    private IntegerField exerciseField;
    private IntegerField gtgField;
    private IntegerField meditateField;
    private IntegerField meetingsField;
    private IntegerField prayField;
    private IntegerField readField;
    private IntegerField sponsorField;

    public StatsView(PostService postService, UserService userService) {
        this.userService = userService;
        this.postService = postService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(true);
        
        initializePost();
        createStatsView();
    }

    private void initializePost() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String email = oidcUser.getEmail();
            
            User user = userService.getUserByEmail(email);
            
            // Try to get existing in-progress post or create new one
            currentPost = postService.getOptionalInProgressPost(user)
                    .orElseGet(() -> postService.startPost(user));
        } else {
            // Fallback for non-authenticated users (shouldn't happen due to route protection)
            currentPost = new Post();
        }
    }

    private void createStatsView() {
        H1 title = new H1("Stats");
        title.getStyle().set("color", "var(--lumo-primary-color)");
        title.getStyle().set("margin-bottom", "2rem");
        
        H3 subtitle = new H3("Track your daily activities");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("margin-bottom", "2rem");

        VerticalLayout statsContainer = new VerticalLayout();
        statsContainer.setAlignItems(Alignment.CENTER);
        statsContainer.setWidth("600px");
        statsContainer.setSpacing(true);

        // Create stat controls
        exerciseField = createStatControl("Exercise", currentPost.getStats().getExercise(), statsContainer);
        gtgField = createStatControl("GTG (Go To Guy)", currentPost.getStats().getGtg(), statsContainer);
        meditateField = createStatControl("Meditate", currentPost.getStats().getMeditate(), statsContainer);
        meetingsField = createStatControl("Meetings", currentPost.getStats().getMeetings(), statsContainer);
        prayField = createStatControl("Pray", currentPost.getStats().getPray(), statsContainer);
        readField = createStatControl("Read", currentPost.getStats().getRead(), statsContainer);
        sponsorField = createStatControl("Sponsor", currentPost.getStats().getSponsor(), statsContainer);

        add(title, subtitle, statsContainer);
    }

    private IntegerField createStatControl(String label, Integer initialValue, VerticalLayout container) {
        // Create up/down buttons
        Button upButton = new Button(new Icon(VaadinIcon.ARROW_UP));
        Button downButton = new Button(new Icon(VaadinIcon.ARROW_DOWN));
        
        // Create the input field
        IntegerField field = new IntegerField();
        field.setValue(initialValue != null ? initialValue : 0);
        field.setMin(0);
        field.setMax(99);
        field.setWidth("50px");
        field.setLabel(null);
        
        upButton.addClickListener(e -> {
            Integer currentValue = field.getValue();
            if (currentValue == null) currentValue = 0;
            if (currentValue < 99) {
                field.setValue(currentValue + 1);
                updateStat(label, currentValue + 1);
            }
        });
        
        downButton.addClickListener(e -> {
            Integer currentValue = field.getValue();
            if (currentValue == null) currentValue = 0;
            if (currentValue > 0) {
                field.setValue(currentValue - 1);
                updateStat(label, currentValue - 1);
            }
        });
        
        // Handle direct input
        field.addValueChangeListener(e -> {
            Integer value = e.getValue();
            if (value != null) {
                if (value < 0) {
                    field.setValue(0);
                    value = 0;
                } else if (value > 99) {
                    field.setValue(99);
                    value = 99;
                }
                updateStat(label, value);
            }
        });
        
        // Create horizontal layout: down arrow | input field | up arrow
        HorizontalLayout controlLayout = new HorizontalLayout();
        controlLayout.setAlignItems(Alignment.CENTER);
        controlLayout.setSpacing(true);
        controlLayout.add(downButton, field, upButton);
        
        // Create a container with the label and control
        VerticalLayout statContainer = new VerticalLayout();
        statContainer.setAlignItems(Alignment.CENTER);
        statContainer.setSpacing(false);
        
        H3 labelElement = new H3(label);
        labelElement.getStyle().set("margin", "0");
        labelElement.getStyle().set("font-size", "var(--lumo-font-size-s)");
        labelElement.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        statContainer.add(labelElement, controlLayout);
        
        // Add the stat container to the passed container
        container.add(statContainer);
        
        return field;
    }

    private void updateStat(String statName, Integer value) {
        if (currentPost != null && currentPost.getStats() != null) {
            Stats stats = currentPost.getStats();
            
            switch (statName.toLowerCase()) {
                case "exercise":
                    stats.setExercise(value);
                    break;
                case "gtg (go to guy)":
                case "gtg":
                    stats.setGtg(value);
                    break;
                case "meditate":
                    stats.setMeditate(value);
                    break;
                case "meetings":
                    stats.setMeetings(value);
                    break;
                case "pray":
                    stats.setPray(value);
                    break;
                case "read":
                    stats.setRead(value);
                    break;
                case "sponsor":
                    stats.setSponsor(value);
                    break;
            }
            
            // Save the post
            try {
                postService.savePost(currentPost);
                Notification.show("Stats updated", 1000, Notification.Position.TOP_CENTER);
            } catch (Exception e) {
                Notification.show("Error saving stats: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            }
        }
    }
} 