package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("TNRA - The Nerdy Retrospective App")
@Route(value = "")
@RouteAlias(value = "main")
@AnonymousAllowed
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
        title.getStyle().set("color", "var(--lumo-primary-color)");
        
        H2 subtitle = new H2("The Nerdy Retrospective App");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Paragraph description = new Paragraph(
            "TNRA helps you conduct effective retrospectives with a structured approach. " +
            "Please log in to access your retrospective sessions."
        );
        description.getStyle().set("text-align", "center");
        description.getStyle().set("max-width", "600px");
        
        Button loginButton = new Button("Login with OIDC", e -> {
            getUI().ifPresent(ui -> ui.getPage().setLocation("/oauth2/authorization/okta"));
        });
        loginButton.getStyle().set("margin-top", "2rem");
        
        add(title, subtitle, description, loginButton);
    }
    
    private void showAuthenticatedView(Authentication authentication) {
        try {
            H1 title = new H1("Welcome back!");
            title.getStyle().set("color", "var(--lumo-primary-color)");
            
            // Get user's display name using the service
            String displayName = oidcUserService.getDisplayName(authentication);
            
            Paragraph welcomeMessage = new Paragraph(
                "Hello, " + displayName + "! You are now logged in."
            );
            welcomeMessage.getStyle().set("text-align", "center");
            
            Button logoutButton = new Button("Logout", event -> {
                getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
            });
            logoutButton.getStyle().set("margin-top", "2rem");
            
            add(title, welcomeMessage, logoutButton);
        } catch (Exception e) {
            // Fallback to simple view if there's an error
            H1 title = new H1("Welcome back!");
            title.getStyle().set("color", "var(--lumo-primary-color)");
            
            Paragraph welcomeMessage = new Paragraph(
                "Hello! You are now logged in."
            );
            welcomeMessage.getStyle().set("text-align", "center");
            
            Paragraph errorMessage = new Paragraph(
                "Note: Could not retrieve user details due to an error."
            );
            errorMessage.getStyle().set("color", "var(--lumo-error-color)");
            errorMessage.getStyle().set("text-align", "center");
            
            Button logoutButton = new Button("Logout", event -> {
                getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
            });
            logoutButton.getStyle().set("margin-top", "2rem");
            
            add(title, welcomeMessage, errorMessage, logoutButton);
            
            // Log the error for debugging
            System.err.println("Error in showAuthenticatedView: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 