package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("Admin Dashboard - TNRA")
@Route(value = "admin", layout = MainLayout.class)
@RolesAllowed("ADMIN")
@CssImport("./styles/admin-view.css")
public class AdminView extends VerticalLayout {

    private final OidcUserService oidcUserService;
    private final UserService userService;

    public AdminView(OidcUserService oidcUserService, UserService userService) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        
        addClassName("admin-view");
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        
        createHeader();
        createContent();
    }

    private void createHeader() {
        H1 title = new H1("Admin Dashboard");
        title.addClassName("admin-title");
        
        Paragraph subtitle = new Paragraph("Administrative tools and system management");
        subtitle.addClassName("admin-subtitle");
        
        add(title, subtitle);
    }

    private void createContent() {
        // User management section
        createUserManagementSection();
        
        // System information section
        createSystemInfoSection();
        
        // Quick actions section
        createQuickActionsSection();
    }

    private void createUserManagementSection() {
        H2 userManagementHeader = new H2("User Management");
        userManagementHeader.addClassName("section-header");
        
        Button viewUsersBtn = new Button("View All Users", VaadinIcon.USERS.create());
        viewUsersBtn.addClassName("admin-button");
        viewUsersBtn.addClickListener(e -> {
            // TODO: Navigate to user management view
            Notification.show("User management functionality coming soon");
        });
        
        Button exportUsersBtn = new Button("Export User Data", VaadinIcon.DOWNLOAD.create());
        exportUsersBtn.addClassName("admin-button");
        exportUsersBtn.addClickListener(e -> {
            // TODO: Implement user data export
            Notification.show("User data export functionality coming soon");
        });
        
        HorizontalLayout userActions = new HorizontalLayout(viewUsersBtn, exportUsersBtn);
        userActions.addClassName("action-layout");
        
        add(userManagementHeader, userActions);
    }

    private void createSystemInfoSection() {
        H2 systemInfoHeader = new H2("System Information");
        systemInfoHeader.addClassName("section-header");
        
        // Display current user's authorities for debugging
        StringBuilder authInfo = new StringBuilder("Current user authorities: ");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                authInfo.append(authority.getAuthority()).append(" ");
            }
        } else {
            authInfo.append("No authentication found");
        }
        Paragraph authDisplay = new Paragraph(authInfo.toString());
        authDisplay.addClassName("system-info");
        
        Button systemStatsBtn = new Button("View System Stats", VaadinIcon.CHART.create());
        systemStatsBtn.addClassName("admin-button");
        systemStatsBtn.addClickListener(e -> {
            // TODO: Navigate to system stats view
            Notification.show("System statistics functionality coming soon");
        });
        
        add(systemInfoHeader, authDisplay, systemStatsBtn);
    }

    private void createQuickActionsSection() {
        H2 quickActionsHeader = new H2("Quick Actions");
        quickActionsHeader.addClassName("section-header");
        
        Button clearCacheBtn = new Button("Clear Cache", VaadinIcon.REFRESH.create());
        clearCacheBtn.addClassNames("admin-button", "warning");
        clearCacheBtn.addClickListener(e -> {
            // TODO: Implement cache clearing
            Notification.show("Cache clearing functionality coming soon");
        });
        
        Button backupDataBtn = new Button("Backup Data", VaadinIcon.DATABASE.create());
        backupDataBtn.addClassName("admin-button");
        backupDataBtn.addClickListener(e -> {
            // TODO: Implement data backup
            Notification.show("Data backup functionality coming soon");
        });
        
        HorizontalLayout quickActions = new HorizontalLayout(clearCacheBtn, backupDataBtn);
        quickActions.addClassName("action-layout");
        
        add(quickActionsHeader, quickActions);
    }
}