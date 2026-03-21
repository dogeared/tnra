package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.AuthNavigationService;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainViewTest {

    @Mock
    private OidcUserService oidcUserService;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuthNavigationService authNavigationService;

    private User testUser;
    private MainView mainView;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlackUserId("test-user");
        testUser.setSlackUsername("testuser");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setProfileImage("profile.jpg");

        lenient().when(oidcUserService.getEmail()).thenReturn("test@example.com");
        lenient().when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        lenient().when(fileStorageService.getFileUrl(anyString())).thenReturn("http://example.com/profile.jpg");
        lenient().when(authNavigationService.getLoginPath()).thenReturn("/oauth2/authorization/okta");
    }

    @Test
    void testMainViewWithUnauthenticatedUser() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
        
        // Check if it contains welcome elements
        boolean hasWelcomeTitle = mainView.getChildren()
            .anyMatch(component -> component instanceof H1);
        assertTrue(hasWelcomeTitle, "Should have welcome title for unauthenticated users");
    }

    @Test
    void testMainViewWithAuthenticatedUser() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(true);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
        
        // Should contain authenticated user elements
        boolean hasUserContent = mainView.getChildren().count() > 0;
        assertTrue(hasUserContent, "Should have content for authenticated users");
    }

    @Test
    void testMainViewLayoutProperties() {
        // Arrange
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertEquals(MainView.Alignment.CENTER, mainView.getAlignItems());
        assertEquals(MainView.JustifyContentMode.CENTER, mainView.getJustifyContentMode());
    }

    @Test
    void testMainViewConstructorWithServices() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert - Constructor should complete without throwing
        assertNotNull(mainView);
    }

    @Test
    void testMainViewHandlesNullUserGracefully() {
        // Arrange
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);
        lenient().when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(null);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);
        });
    }

    @Test
    void testMainViewWithUserWithoutProfileImage() {
        // Arrange
        testUser.setProfileImage(null);
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);
        lenient().when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }

    @Test
    void testMainViewWithUserWithProfileImage() {
        // Arrange
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);
        lenient().when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }

    @Test
    void testMainViewSizeFull() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        // We can verify the view was created without exception
        assertNotNull(mainView);
    }

    @Test
    void testMainViewWithFileStorageServiceUnavailable() {
        // Arrange
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);
        lenient().when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(fileStorageService.getFileUrl(anyString())).thenReturn(null);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }

    @Test
    void testMainViewWithPartialUserData() {
        // Arrange
        testUser.setFirstName(null);
        testUser.setLastName("Doe");
        lenient().when(oidcUserService.isAuthenticated()).thenReturn(true);
        lenient().when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }

    @Test
    void testMainViewFallsBackToCurrentUserNameWhenDisplayNameMissing() {
        testUser.setProfileImage(null);
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getDisplayName()).thenReturn(" ");
        when(userService.getCurrentUser()).thenReturn(testUser);

        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        boolean hasFallbackGreeting = mainView.getChildren()
            .flatMap(component -> component.getChildren())
            .filter(component -> component instanceof Paragraph)
            .map(component -> (Paragraph) component)
            .anyMatch(paragraph -> paragraph.getText().contains("Hello, John Doe!"));

        assertTrue(hasFallbackGreeting, "Expected greeting to use current user name when display name is blank");
    }

    @Test
    void testMainViewSetsAccessibleAltTextForProfileImage() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);

        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        Image profileImage = mainView.getChildren()
            .flatMap(component -> component.getChildren())
            .filter(component -> component instanceof Image)
            .map(component -> (Image) component)
            .findFirst()
            .orElseThrow();

        assertEquals("User profile image", profileImage.getAlt().orElse(null));
        verify(fileStorageService).getFileUrl("profile.jpg");
    }

    @Test
    void testMainViewShowsLoginButtonForUnauthenticatedUsers() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        boolean hasLoginButton = mainView.getChildren()
            .anyMatch(component -> component instanceof Button && "Log in".equals(((Button) component).getText()));

        assertTrue(hasLoginButton, "Expected unauthenticated view to include a log in button");
    }

    @Test
    void testAuthenticatedViewShowsQuickActionButtons() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getDisplayName()).thenReturn("Test User");
        when(userService.getCurrentUser()).thenReturn(testUser);

        mainView = new MainView(oidcUserService, userService, fileStorageService, authNavigationService);

        long quickActionCount = mainView.getChildren()
            .flatMap(component -> component.getChildren())
            .filter(component -> component instanceof Button)
            .map(component -> (Button) component)
            .map(Button::getText)
            .filter(text -> "Go to Posts".equals(text) || "View Profile".equals(text) || "Edit Stats".equals(text))
            .count();

        assertEquals(3, quickActionCount, "Expected authenticated main view to show all quick action buttons");
    }
}
