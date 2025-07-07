package com.afitnerd.tnra.vaadin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
@CssImport("./styles/stats-view.css")
public class StatsView extends VerticalLayout {

    private final PostService postService;
    private final UserService userService;
    private Post currentPost;
    private List<StatCard> statCards = new ArrayList<>();
    private boolean isReadOnly = false;
    private Button toggleButton;

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
            throw new RuntimeException("User is not authenticated");
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
        header.addClassName("stats-header");

        // Title and date row
        H1 title = new H1("Daily Stats");
        title.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD, "stats-title");

        String startDate = "Session started " + formatDateTime(currentPost.getStart());
        Span dateSpan = new Span(startDate);
        dateSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, "stats-date");

        // Toggle button row
        HorizontalLayout toggleRow = new HorizontalLayout();
        toggleRow.setAlignItems(Alignment.CENTER);
        toggleRow.setSpacing(true);
        toggleRow.setPadding(false);
        toggleRow.addClassName("toggle-row");

        toggleButton = new Button();
        toggleButton.addClassNames(
            LumoUtility.BorderRadius.MEDIUM,
            LumoUtility.Padding.SMALL,
            "toggle-button"
        );
        updateToggleButton();
        
        toggleButton.addClickListener(e -> {
            isReadOnly = !isReadOnly;
            setReadOnly(isReadOnly);
            updateToggleButton();
            showSuccessNotification(isReadOnly ? "Stats are now read-only" : "Stats are now editable");
        });

        toggleRow.add(toggleButton);
        header.add(title, dateSpan, toggleRow);
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
        grid.addClassName("stats-grid");

        // Clear previous stat cards
        statCards.clear();

        // Create stat cards
        StatCard exerciseCard = new StatCard("Exercise", "💪", currentPost.getStats().getExercise());
        StatCard meditateCard = new StatCard("Meditate", "🧘", currentPost.getStats().getMeditate());
        StatCard prayCard = new StatCard("Pray", "🙏", currentPost.getStats().getPray());
        StatCard readCard = new StatCard("Read", "📚", currentPost.getStats().getRead());
        StatCard gtgCard = new StatCard("GTG", "👥", currentPost.getStats().getGtg());
        StatCard meetingsCard = new StatCard("Meetings", "🤝", currentPost.getStats().getMeetings());
        StatCard sponsorCard = new StatCard("Sponsor", "🤲", currentPost.getStats().getSponsor());

        // Add value change listeners
        exerciseCard.setValueChangeListener(value -> updateStat("Exercise", value));
        meditateCard.setValueChangeListener(value -> updateStat("Meditate", value));
        prayCard.setValueChangeListener(value -> updateStat("Pray", value));
        readCard.setValueChangeListener(value -> updateStat("Read", value));
        gtgCard.setValueChangeListener(value -> updateStat("GTG", value));
        meetingsCard.setValueChangeListener(value -> updateStat("Meetings", value));
        sponsorCard.setValueChangeListener(value -> updateStat("Sponsor", value));

        // Add to list for later access
        statCards.addAll(List.of(exerciseCard, meditateCard, prayCard, readCard, gtgCard, meetingsCard, sponsorCard));

        // Add all cards to grid
        grid.add(exerciseCard, meditateCard, prayCard, readCard, gtgCard, meetingsCard, sponsorCard);

        return grid;
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
        notification.addThemeName("primary");
    }
    
    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        for (StatCard card : statCards) {
            card.setReadOnly(readOnly);
        }
    }
    
    private void updateToggleButton() {
        if (isReadOnly) {
            toggleButton.setIcon(VaadinIcon.EDIT.create());
            toggleButton.setText("Enable Editing");
            toggleButton.addThemeName("primary");
        } else {
            toggleButton.setIcon(VaadinIcon.EYE.create());
            toggleButton.setText("Read Only");
            toggleButton.removeThemeName("primary");
        }
    }
    
    public void refreshStats() {
        if (currentPost != null && currentPost.getStats() != null) {
            Stats stats = currentPost.getStats();
            
            // Find and update each stat card
            for (StatCard card : statCards) {
                switch (card.getLabel().toLowerCase()) {
                    case "exercise":
                        card.setValue(stats.getExercise());
                        break;
                    case "meditate":
                        card.setValue(stats.getMeditate());
                        break;
                    case "pray":
                        card.setValue(stats.getPray());
                        break;
                    case "read":
                        card.setValue(stats.getRead());
                        break;
                    case "gtg":
                        card.setValue(stats.getGtg());
                        break;
                    case "meetings":
                        card.setValue(stats.getMeetings());
                        break;
                    case "sponsor":
                        card.setValue(stats.getSponsor());
                        break;
                }
            }
        }
    }
    
    private String formatDateTime(Date date) {
        if (date == null) {
            return "Unknown";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a");
        return formatter.format(date);
    }
} 