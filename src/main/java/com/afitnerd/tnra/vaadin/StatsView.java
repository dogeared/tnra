package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.text.SimpleDateFormat;
import java.util.Date;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
public class StatsView extends VerticalLayout {

    private final PostService postService;
    private final UserService userService;
    private Post currentPost;
    private boolean isUpdatingFromButton = false;

    public StatsView(PostService postService, UserService userService) {
        this.userService = userService;
        this.postService = postService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(false);
        
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
        // Header section
        VerticalLayout headerSection = createHeaderSection();
        
        // Stats grid
        HorizontalLayout statsGrid = createStatsGrid();
        
        add(headerSection, statsGrid);
    }

    private VerticalLayout createHeaderSection() {
        VerticalLayout header = new VerticalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(false);
        header.setPadding(false);
        header.setWidth("100%");
        header.getStyle().set("margin-bottom", "1rem");

        H1 title = new H1("Daily Stats");
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);
        title.getStyle().set("color", "var(--lumo-primary-color)");
        title.getStyle().set("margin", "0");

        String startDate = "Session started " + formatDateTime(currentPost.getStart());
        Span dateSpan = new Span(startDate);
        dateSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        dateSpan.getStyle().set("margin-top", "0.25rem");

        header.add(title, dateSpan);
        return header;
    }

    private HorizontalLayout createStatsGrid() {
        HorizontalLayout grid = new HorizontalLayout();
        grid.setWidth("100%");
        grid.setMaxWidth("600px");
        grid.setSpacing(false);
        grid.setPadding(false);
        grid.setAlignItems(Alignment.START);
        grid.setJustifyContentMode(JustifyContentMode.CENTER);
        grid.getStyle().set("flex-wrap", "wrap");

        // Add all stats in a single row that wraps
        grid.add(
            createStatCard("Exercise", "💪", currentPost.getStats().getExercise()),
            createStatCard("Meditate", "🧘", currentPost.getStats().getMeditate()),
            createStatCard("Pray", "🙏", currentPost.getStats().getPray()),
            createStatCard("Read", "📚", currentPost.getStats().getRead()),
            createStatCard("GTG", "👥", currentPost.getStats().getGtg()),
            createStatCard("Meetings", "🤝", currentPost.getStats().getMeetings()),
            createStatCard("Sponsor", "🤲", currentPost.getStats().getSponsor())
        );

        return grid;
    }

    private Div createStatCard(String label, String emoji, Integer initialValue) {
        Div card = new Div();
        card.addClassNames(
            LumoUtility.Background.CONTRAST_5,
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL
        );
        card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");
        card.getStyle().set("min-width", "140px");
        card.getStyle().set("max-width", "160px");
        card.getStyle().set("text-align", "center");
        card.getStyle().set("margin", "0.25rem");

        // Card header with emoji and label
        Div header = new Div();
        header.getStyle().set("margin-bottom", "0.5rem");
        
        Span emojiSpan = new Span(emoji);
        emojiSpan.getStyle().set("font-size", "1.5rem");
        emojiSpan.getStyle().set("display", "block");
        emojiSpan.getStyle().set("margin-bottom", "0.25rem");
        
        Span labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.FontSize.SMALL);
        labelSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
        
        header.add(emojiSpan, labelSpan);

        // Value display - clickable and editable
        IntegerField valueField = new IntegerField();
        valueField.setValue(initialValue != null ? initialValue : 0);
        valueField.setMin(0);
        valueField.setMax(99);
        valueField.setWidth("50px");
        valueField.setLabel(null);
        valueField.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        valueField.getStyle().set("color", "var(--lumo-primary-color)");
        valueField.getStyle().set("text-align", "center");
        valueField.getStyle().set("margin", "0.25rem 0");

        // Control buttons
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(false);
        controls.setAlignItems(Alignment.CENTER);
        controls.setJustifyContentMode(JustifyContentMode.CENTER);

        Button minusBtn = createControlButton(VaadinIcon.MINUS, "Decrease " + label);
        Button plusBtn = createControlButton(VaadinIcon.PLUS, "Increase " + label);

        // Store current value for button handlers
        final int[] currentValue = {initialValue != null ? initialValue : 0};

        minusBtn.addClickListener(e -> {
            if (currentValue[0] > 0) {
                currentValue[0]--;
                isUpdatingFromButton = true;
                valueField.setValue(currentValue[0]);
                updateStat(label, currentValue[0]);
                addPulseAnimation(valueField);
                isUpdatingFromButton = false;
            }
        });

        plusBtn.addClickListener(e -> {
            if (currentValue[0] < 99) {
                currentValue[0]++;
                isUpdatingFromButton = true;
                valueField.setValue(currentValue[0]);
                updateStat(label, currentValue[0]);
                addPulseAnimation(valueField);
                isUpdatingFromButton = false;
            }
        });

        // Handle direct input
        valueField.addValueChangeListener(e -> {
            // Skip if this change was triggered by a button click
            if (isUpdatingFromButton) {
                return;
            }
            
            Integer value = e.getValue();
            if (value != null) {
                if (value < 0) {
                    valueField.setValue(0);
                    value = 0;
                } else if (value > 99) {
                    valueField.setValue(99);
                    value = 99;
                }
                currentValue[0] = value;
                updateStat(label, value);
            }
        });

        controls.add(minusBtn, valueField, plusBtn);
        card.add(header, controls);

        return card;
    }

    private Button createControlButton(VaadinIcon icon, String ariaLabel) {
        Button button = new Button(icon.create());
        button.addClassNames(
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL
        );
        button.getStyle().set("min-width", "32px");
        button.getStyle().set("min-height", "32px");
        button.getStyle().set("background-color", "var(--lumo-primary-color)");
        button.getStyle().set("color", "white");
        button.getStyle().set("border", "none");
        button.getStyle().set("cursor", "pointer");
        button.setAriaLabel(ariaLabel);

        // Hover effects
        button.getElement().getStyle().set("transition", "all 0.2s ease");
        button.addClickListener(e -> {
            button.getStyle().set("transform", "scale(0.95)");
            button.getElement().executeJs("setTimeout(() => $0.style.transform = 'scale(1)', 100)");
        });

        return button;
    }

    private void addPulseAnimation(com.vaadin.flow.component.Component element) {
        element.getStyle().set("animation", "pulse 0.3s ease-in-out");
        element.getElement().executeJs("setTimeout(() => $0.style.animation = '', 300)");
    }

    private void updateStat(String statName, Integer value) {
        if (currentPost != null && currentPost.getStats() != null) {
            Stats stats = currentPost.getStats();
            
            switch (statName.toLowerCase()) {
                case "exercise":
                    stats.setExercise(value);
                    break;
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
                showSuccessNotification(statName + " updated to " + value);
            } catch (Exception e) {
                Notification.show("Error saving stats: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            }
        }
    }

    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 1500, Notification.Position.TOP_CENTER);
        notification.addThemeName("success");
    }
    
    private String formatDateTime(Date date) {
        if (date == null) {
            return "Unknown";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a");
        return formatter.format(date);
    }
} 