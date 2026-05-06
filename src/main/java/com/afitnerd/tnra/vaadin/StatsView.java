package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.VaadinPostPresenter;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
import java.util.List;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
@PermitAll
@CssImport("./styles/stats-view.css")
public class StatsView extends VerticalLayout implements AfterNavigationObserver {

    private final VaadinPostPresenter vaadinPostPresenter;
    private Post currentPost;
    private List<StatCard> statCards = new ArrayList<>();
    private List<StatDefinition> statDefinitions = new ArrayList<>();
    private boolean isReadOnly = false;
    private Runnable onStatsChanged;

    public StatsView(VaadinPostPresenter vaadinPostPresenter) {
        this.vaadinPostPresenter = vaadinPostPresenter;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        setPadding(false);
    }

    public static StatsView createEmbedded(VaadinPostPresenter vaadinPostPresenter, User currentUser) {
        StatsView statsView = new StatsView(vaadinPostPresenter);
        List<StatDefinition> allStats = new ArrayList<>(vaadinPostPresenter.getActiveGlobalStatDefinitions());
        allStats.addAll(vaadinPostPresenter.getActivePersonalStatDefinitions(currentUser));
        statsView.statDefinitions = allStats;
        statsView.currentPost = new Post();
        statsView.isReadOnly = true;
        statsView.createStatsView();
        statsView.setReadOnly(statsView.isReadOnly);
        return statsView;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        User currentUser = vaadinPostPresenter.initializeUser();
        List<StatDefinition> allStats = new ArrayList<>(vaadinPostPresenter.getActiveGlobalStatDefinitions());
        allStats.addAll(vaadinPostPresenter.getActivePersonalStatDefinitions(currentUser));
        statDefinitions = allStats;
        initializePost();
        createStatsView();
    }

    private void initializePost() {
        User currentUser = vaadinPostPresenter.initializeUser();
        currentPost = vaadinPostPresenter.getOptionalInProgressPost(currentUser)
            .orElseGet(() -> vaadinPostPresenter.startPost(currentUser));
    }

    private void createStatsView() {
        removeAll();
        VerticalLayout headerSection = createHeaderSection();
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

        H1 title = new H1("Daily Stats");
        title.addClassName("stats-title");

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

        statCards.clear();

        for (StatDefinition statDef : statDefinitions) {
            Integer currentValue = currentPost != null ? currentPost.getStatValue(statDef.getName()) : null;
            String emoji = statDef.getEmoji() != null ? statDef.getEmoji() : "";

            StatCard card = new StatCard(statDef.getLabel(), emoji, currentValue);
            card.setValueChangeListener(value -> updateStat(statDef, value));

            statCards.add(card);
            grid.add(card);
        }

        return grid;
    }

    private void updateStat(StatDefinition statDef, Integer value) {
        if (currentPost != null) {
            try {
                currentPost = vaadinPostPresenter.updateStatValue(statDef, value);
                if (onStatsChanged != null) {
                    onStatsChanged.run();
                }
            } catch (Exception e) {
                AppNotification.error("Error saving stats: " + e.getMessage());
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
        if (currentPost != null) {
            for (int i = 0; i < statCards.size() && i < statDefinitions.size(); i++) {
                StatDefinition statDef = statDefinitions.get(i);
                Integer value = currentPost.getStatValue(statDef.getName());
                statCards.get(i).setValue(value);
            }
        }
    }

    public void setPost(Post post) {
        this.currentPost = post;
        if (post != null) {
            refreshStats();
        } else {
            for (StatCard card : statCards) {
                card.setValue(null);
            }
        }
    }

    public void loadFromPost(Post post) {
        this.currentPost = post;
        if (post == null || post.getStatValues() == null) return;

        // Derive stat definitions from the post's actual values (globals first, then personals)
        List<StatDefinition> defs = post.getStatValues().stream()
            .map(PostStatValue::getStatDefinition)
            .sorted((a, b) -> {
                boolean aGlobal = !(a instanceof PersonalStatDefinition);
                boolean bGlobal = !(b instanceof PersonalStatDefinition);
                if (aGlobal != bGlobal) return aGlobal ? -1 : 1;
                return a.getDisplayOrder().compareTo(b.getDisplayOrder());
            })
            .toList();
        this.statDefinitions = new ArrayList<>(defs);
        createStatsView();
        refreshStats();
    }

    public void flushPendingValues() {
        if (currentPost == null) return;
        for (int i = 0; i < statCards.size() && i < statDefinitions.size(); i++) {
            StatCard card = statCards.get(i);
            StatDefinition statDef = statDefinitions.get(i);
            Integer cardValue = card.getValue();
            Integer dbValue = currentPost.getStatValue(statDef.getName());
            if ((cardValue != null && !cardValue.equals(dbValue)) || (cardValue == null && dbValue != null)) {
                currentPost = vaadinPostPresenter.updateStatValue(statDef, cardValue);
            }
        }
    }

    public boolean areAllStatsSet() {
        if (currentPost == null) {
            return false;
        }
        for (StatDefinition statDef : statDefinitions) {
            if (currentPost.getStatValue(statDef.getName()) == null) {
                return false;
            }
        }
        return true;
    }

    public void setOnStatsChanged(Runnable callback) {
        this.onStatsChanged = callback;
    }

    List<StatCard> getStatCards() {
        return statCards;
    }
}
