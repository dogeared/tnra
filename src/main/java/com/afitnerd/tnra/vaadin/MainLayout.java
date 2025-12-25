package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class MainLayout extends AppLayout {

    private final OidcUserService oidcUserService;

    public MainLayout(OidcUserService oidcUserService) {
        this.oidcUserService = oidcUserService;
        createHeader();
        createDrawer();
        
        // Make the drawer collapsible by default and show the toggle button
        setDrawerOpened(false);
        setPrimarySection(Section.DRAWER);
    }

    private void createHeader() {
        DrawerToggle drawerToggle = new DrawerToggle();
        
        H1 logo = new H1("TNRA");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM);

        Button authButton;
        if (oidcUserService.isAuthenticated()) {
            authButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> {
                getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
            });
        } else {
            authButton = new Button("Login", VaadinIcon.SIGN_IN.create(), e -> {
                getUI().ifPresent(ui -> ui.getPage().setLocation("/oauth2/authorization/oidc"));
            });
        }
        authButton.addClassNames(LumoUtility.Margin.MEDIUM);

        HorizontalLayout header = new HorizontalLayout(drawerToggle, logo, authButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames(
            LumoUtility.Gap.SMALL,
            LumoUtility.Display.FLEX,
            LumoUtility.FlexDirection.COLUMN,
            LumoUtility.Height.FULL);

        // Home tab - always visible
        Tab homeTab = createTab("Home", VaadinIcon.HOME, MainView.class);
        tabs.add(homeTab);

        // Stats tab - only visible when authenticated
        if (oidcUserService.isAuthenticated()) {
            Tab statsTab = createTab("Stats", VaadinIcon.CHART_LINE, StatsView.class);
            tabs.add(statsTab);

            // Posts tab - only visible when authenticated
            Tab postsTab = createTab("Posts", VaadinIcon.FILE_TEXT, PostView.class);
            tabs.add(postsTab);

            // GTG tab - only visible when authenticated
            Tab gtgTab = createTab("Go To Guy", VaadinIcon.PHONE, GTGView.class);
            tabs.add(gtgTab);

            // Profile tab - only visible when authenticated
            Tab profileTab = createTab("Profile", VaadinIcon.USER, ProfileView.class);
            tabs.add(profileTab);
            
            // Admin tab - only visible to users with ADMIN role
            if (hasAdminRole()) {
                Tab adminTab = createTab("Admin", VaadinIcon.COG, AdminView.class);
                tabs.add(adminTab);
            }
        }

        addToDrawer(tabs);
    }

    private Tab createTab(String text, VaadinIcon viewIcon, Class<?> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
            .set("margin-inline-end", "var(--lumo-space-m)")
            .set("margin-inline-start", "var(--lumo-space-xs)")
            .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(text));
        link.setRoute((Class) navigationTarget);
        link.setTabIndex(-1);

        return new Tab(link);
    }
    
    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()) || 
                                 "ADMIN".equals(authority.getAuthority()));
    }
} 