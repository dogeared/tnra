package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("TNRA - The Nerdy Retrospective App")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "main", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/main-view.css")
public class MainView extends VerticalLayout {

    private final OidcUserService oidcUserService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public MainView(OidcUserService oidcUserService, UserService userService, FileStorageService fileStorageService) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        if (oidcUserService.isAuthenticated()) {
            showAuthenticatedView();
        } else {
            showUnauthenticatedView();
        }
    }
    
    private void showUnauthenticatedView() {
        H1 title = new H1("Welcome to TNRA");
        title.addClassName("main-title");
        
        H2 subtitle = new H2("The Taking the Next Right Action App");
        subtitle.addClassName("main-subtitle");
        
        Paragraph description = new Paragraph(
            "Please log in to access your posts!"
        );
        description.addClassName("main-description");
        
        add(title, subtitle, description);
    }
    
    private void showAuthenticatedView() {
        try {
            H1 title = new H1("Welcome back!");
            title.addClassName("main-title");
            
            // Get user's display name using the service
            String displayName = oidcUserService.getDisplayName();
            
            // Get current user to access profile image
            User currentUser = userService.getCurrentUser();
            
            // Create profile section with image and welcome message
            HorizontalLayout profileSection = new HorizontalLayout();
            profileSection.setAlignItems(Alignment.CENTER);
            profileSection.setSpacing(true);
            profileSection.addClassName("profile-section");
            
            // Profile Image
            Image profileImage = new Image();
            profileImage.addClassName("main-profile-image");
            
            if (currentUser != null && currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                String imageUrl = fileStorageService.getFileUrl(currentUser.getProfileImage());
                profileImage.setSrc(imageUrl);
            } else {
                profileImage.setSrc("/uploads/placeholder.png");
            }
            
            // Welcome Message
            Paragraph welcomeMessage = new Paragraph(
                "Hello, " + displayName + "! You are now logged in."
            );
            welcomeMessage.addClassName("welcome-message");
            
            profileSection.add(profileImage, welcomeMessage);
            
            add(title, profileSection);
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