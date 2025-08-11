package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.VaadinPostService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
@CssImport("./styles/stats-view.css")
public class StatsView extends VerticalLayout implements AfterNavigationObserver {

    private final VaadinPostService vaadinPostService;
    private Post currentPost;
    private List<StatCard> statCards = new ArrayList<>();
    private boolean isReadOnly = false;
    private Runnable onStatsChanged;

    public StatsView(VaadinPostService vaadinPostService) {
        this.vaadinPostService = vaadinPostService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(false);
    }

    // Static factory method for creating embedded StatsView
    public static StatsView createEmbedded(VaadinPostService vaadinPostService) {
        StatsView statsView = new StatsView(vaadinPostService);
        statsView.currentPost = new Post(); // Start with empty post
        statsView.isReadOnly = true;
        statsView.createStatsView();
        statsView.setReadOnly(statsView.isReadOnly);
        return statsView;
    }

    // only reachable if NOT embedded
    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        initializePost();
        createStatsView();
    }

    private void initializePost() {
        User currentUser = vaadinPostService.initializeUser();
        currentPost = vaadinPostService.getOptionalInProgressPost(currentUser)
            .orElseGet(() -> vaadinPostService.startPost(currentUser));
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

        header.add(title);
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

        // Create stat cards - new posts will have null stats, so inputs start empty
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
            // Create a new Stats object with the updated value
            Stats stats = new Stats();
            
            // Copy current values
            Stats currentStats = currentPost.getStats();
            stats.setExercise(currentStats.getExercise());
            stats.setGtg(currentStats.getGtg());
            stats.setMeditate(currentStats.getMeditate());
            stats.setMeetings(currentStats.getMeetings());
            stats.setPray(currentStats.getPray());
            stats.setRead(currentStats.getRead());
            stats.setSponsor(currentStats.getSponsor());
            
            // Update the specific stat
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
            
            // Use updateCompleteStats to update stats (allows null values for unsetting)
            try {
                currentPost = vaadinPostService.updateCompleteStats(stats);
                String displayValue = value != null ? value.toString() : "empty";
                
                // Notify parent that stats have changed
                if (onStatsChanged != null) {
                    onStatsChanged.run();
                }
            } catch (Exception e) {
                Notification.show("Error saving stats: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER);
            }
        }
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        for (StatCard card : statCards) {
            card.setReadOnly(readOnly);
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

    // Method to set the post externally (for embedded use)
    public void setPost(Post post) {
        this.currentPost = post;
        if (post != null && post.getStats() != null) {
            refreshStats();
        } else {
            // Clear all stat cards when post is null
            for (StatCard card : statCards) {
                card.setValue(null);
            }
        }
    }
    
    /**
     * Check if all stats have been set (not null)
     */
    public boolean areAllStatsSet() {
        if (currentPost == null || currentPost.getStats() == null) {
            return false;
        }
        
        Stats stats = currentPost.getStats();
        return stats.getExercise() != null &&
               stats.getMeditate() != null &&
               stats.getPray() != null &&
               stats.getRead() != null &&
               stats.getGtg() != null &&
               stats.getMeetings() != null &&
               stats.getSponsor() != null;
    }
    
    /**
     * Set callback to be called when stats change
     */
    public void setOnStatsChanged(Runnable callback) {
        this.onStatsChanged = callback;
    }
} 