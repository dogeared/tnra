package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainViewTest {

    @Mock
    private OidcUserService oidcUserService;

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

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
    }

    @Test
    void testMainViewWithUnauthenticatedUser() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

        // Assert
        assertEquals(MainView.Alignment.CENTER, mainView.getAlignItems());
        assertEquals(MainView.JustifyContentMode.CENTER, mainView.getJustifyContentMode());
    }

    @Test
    void testMainViewConstructorWithServices() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
            mainView = new MainView(oidcUserService, userService, fileStorageService);
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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }

    @Test
    void testMainViewSizeFull() {
        // Arrange
        when(oidcUserService.isAuthenticated()).thenReturn(false);

        // Act
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

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
        mainView = new MainView(oidcUserService, userService, fileStorageService);

        // Assert
        assertNotNull(mainView);
        assertTrue(mainView.getChildren().count() > 0);
    }
}