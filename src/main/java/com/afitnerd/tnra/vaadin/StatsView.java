package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.VaadinPostPresenter;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


import java.util.ArrayList;
import java.util.List;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
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

    public static StatsView createEmbedded(VaadinPostPresenter vaadinPostPresenter) {
        StatsView statsView = new StatsView(vaadinPostPresenter);
        statsView.statDefinitions = vaadinPostPresenter.getActiveStatDefinitions();
        statsView.currentPost = new Post();
        statsView.isReadOnly = true;
        statsView.createStatsView();
        statsView.setReadOnly(statsView.isReadOnly);
        return statsView;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        statDefinitions = vaadinPostPresenter.getActiveStatDefinitions();
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
}
