package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

@PageTitle("TNRA - Taking the Next Right Action")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "main", layout = MainLayout.class)
@AnonymousAllowed
@CssImport("./styles/main-view.css")
public class MainView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(MainView.class);

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

        Anchor signInLink = new Anchor("/oauth2/authorization/okta", "Sign in to get started");
        signInLink.addClassName("login-cta");
        signInLink.getElement().setAttribute("role", "button");
        
        add(title, subtitle, description, signInLink);
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
            profileImage.setAlt("User profile image");

            if (currentUser != null && currentUser.getProfileImage() != null && !currentUser.getProfileImage().isEmpty()) {
                String imageUrl = fileStorageService.getFileUrl(currentUser.getProfileImage());
                profileImage.setSrc(StringUtils.hasText(imageUrl) ? imageUrl : "/uploads/placeholder.png");
            } else {
                profileImage.setSrc("/uploads/placeholder.png");
            }

            String resolvedDisplayName = resolveDisplayName(displayName, currentUser);

            // Welcome Message
            Paragraph welcomeMessage = new Paragraph(
                "Hello, " + resolvedDisplayName + "! You are now logged in."
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

            log.warn("Error while rendering authenticated main view", e);
        }
    }

    private String resolveDisplayName(String displayName, User currentUser) {
        if (StringUtils.hasText(displayName)) {
            return displayName;
        }
        if (currentUser == null) {
            return "there";
        }
        if (StringUtils.hasText(currentUser.getFirstName()) && StringUtils.hasText(currentUser.getLastName())) {
            return currentUser.getFirstName().trim() + " " + currentUser.getLastName().trim();
        }
        if (StringUtils.hasText(currentUser.getFirstName())) {
            return currentUser.getFirstName().trim();
        }
        if (StringUtils.hasText(currentUser.getEmail())) {
            return currentUser.getEmail().trim();
        }
        return "there";
    }
}
