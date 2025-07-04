package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.dependency.CssImport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("TNRA - The Nerdy Retrospective App")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "main", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/main-view.css")
public class MainView extends VerticalLayout {

    private final OidcUserService oidcUserService;

    public MainView(OidcUserService oidcUserService) {
        this.oidcUserService = oidcUserService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && 
                                 authentication.isAuthenticated() && 
                                 !"anonymousUser".equals(authentication.getName());
        
        if (isAuthenticated) {
            showAuthenticatedView(authentication);
        } else {
            showUnauthenticatedView();
        }
    }
    
    private void showUnauthenticatedView() {
        H1 title = new H1("Welcome to TNRA");
        title.addClassName("main-title");
        
        H2 subtitle = new H2("The Nerdy Retrospective App");
        subtitle.addClassName("main-subtitle");
        
        Paragraph description = new Paragraph(
            "TNRA helps you conduct effective retrospectives with a structured approach. " +
            "Please log in to access your retrospective sessions."
        );
        description.addClassName("main-description");
        
        add(title, subtitle, description);
    }
    
    private void showAuthenticatedView(Authentication authentication) {
        try {
            H1 title = new H1("Welcome back!");
            title.addClassName("main-title");
            
            // Get user's display name using the service
            String displayName = oidcUserService.getDisplayName(authentication);
            
            Paragraph welcomeMessage = new Paragraph(
                "Hello, " + displayName + "! You are now logged in."
            );
            welcomeMessage.addClassName("welcome-message");
            
            add(title, welcomeMessage);
        } catch (Exception e) {
            // Fallback to simple view if there's an error
            H1 title = new H1("Welcome back!");
            title.addClassName("main-title");
            
            Paragraph welcomeMessage = new Paragraph(
                "Hello! You are now logged in."
            );
            welcomeMessage.addClassName("welcome-message");
            
            Paragraph errorMessage = new Paragraph(
                "Note: Could not retrieve user details due to an error."
            );
            errorMessage.addClassName("error-message");
            
            add(title, welcomeMessage, errorMessage);
            
            // Log the error for debugging
            System.err.println("Error in showAuthenticatedView: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 