package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Stats - TNRA")
@Route(value = "stats", layout = MainLayout.class)
public class StatsView extends VerticalLayout {

    public StatsView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        
        H1 title = new H1("Stats");
        title.getStyle().set("color", "var(--lumo-primary-color)");
        
        add(title);
    }
} 