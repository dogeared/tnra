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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
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
        H2 title = new H2("Admin Dashboard");
        title.addClassName("admin-title");
        
        Paragraph subtitle = new Paragraph("Administrative tools and system management");
        subtitle.addClassName("admin-subtitle");
        
        add(title, subtitle);
    }

    private void createContent() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();
        
        // Create GTG tab with existing admin content
        VerticalLayout gtgContent = createGtgTabContent();
        Tab gtgTab = tabSheet.add("GTG", gtgContent);
        
        add(tabSheet);
    }
    
    private VerticalLayout createGtgTabContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();

        return content;
    }
}