package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.VaadinPostPresenter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Test class for PostView that exercises various modes and UI states:
 * 1. In-progress post exists + showing completed view: switch button, dropdown, pagination
 * 2. In-progress post exists + showing in-progress view: switch button, date/time, finish button
 * 3. No in-progress post: completed view only with dropdown, pagination, start button
 * 4. Cannot show in-progress view when no in-progress post exists
 */
@ExtendWith(MockitoExtension.class)
class PostViewTest {

    @Mock
    private VaadinPostPresenter vaadinPostPresenter;
    
    @Mock
    private UI mockUI;

    @Mock
    private VaadinSession mockSession;
    
    @Mock
    private ExtendedClientDetails mockExtendedClientDetails;

    private User testUser;
    private Post inProgressPost;
    private Post completedPost1;
    private Post completedPost2;
    private org.springframework.data.domain.Page<Post> completedPostsPage;

    private PostView postView;

    @BeforeEach
    void setUp() {
        // Create test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setSlackUserId("test-user");
        testUser.setSlackUsername("testuser");

        inProgressPost = new Post();
        inProgressPost.setId(1L);
        inProgressPost.setUser(testUser);
        inProgressPost.setState(PostState.IN_PROGRESS);
        inProgressPost.setStart(new Date());

        completedPost1 = new Post();
        completedPost1.setId(2L);
        completedPost1.setUser(testUser);
        completedPost1.setState(PostState.COMPLETE);
        completedPost1.setStart(new Date(System.currentTimeMillis() - 86400000L)); // 1 day ago
        completedPost1.setFinish(new Date(System.currentTimeMillis() - 82800000L)); // finished 1 hour later

        completedPost2 = new Post();
        completedPost2.setId(3L);
        completedPost2.setUser(testUser);
        completedPost2.setState(PostState.COMPLETE);
        completedPost2.setStart(new Date(System.currentTimeMillis() - 172800000L)); // 2 days ago
        completedPost2.setFinish(new Date(System.currentTimeMillis() - 169200000L)); // finished 1 hour later

        completedPostsPage = new PageImpl<>(Arrays.asList(completedPost1, completedPost2));

        // Setup common mocks with lenient stubbing to avoid unnecessary stubbing warnings
        lenient().when(vaadinPostPresenter.initializeUser()).thenReturn(testUser);
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class))).thenReturn(completedPostsPage);
    }

    @Test
    void testHasInProgressPost_ShowingCompletedView() {
        // Arrange: Has in-progress post, but showing completed view
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());
            
            // Force switch to completed posts view
            postView.showingCompletedPosts = true;
            invokeMethod(postView, "recreateHeader");

            // Assert: Should have switch to in-progress button, dropdown, and pagination
            assertTrue(hasComponent(postView, "Switch to in-progress post"), "Should have 'Switch to in-progress post' button");
            assertTrue(hasComponentOfType(postView, ComboBox.class), "Should have posts dropdown");
            assertTrue(hasPaginationControls(postView), "Should have pagination controls");
            assertFalse(hasComponent(postView, "Start New Post"), "Should NOT have 'Start New Post' button");
            assertFalse(hasComponent(postView, "Finish Post"), "Should NOT have 'Finish Post' button");
        }
    }

    @Test
    void testHasInProgressPost_ShowingInProgressView() {
        // Arrange: Has in-progress post, showing in-progress view
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: Should have switch button, date/time display, and finish button
            assertTrue(hasComponent(postView, "Switch to completed posts"), "Should have 'Switch to completed posts' button");
            assertTrue(hasComponent(postView, "Finish Post"), "Should have 'Finish Post' button");
            assertTrue(hasPostStartedDate(postView), "Should show post started date/time");
            assertFalse(hasComponent(postView, "Start New Post"), "Should NOT have 'Start New Post' button");
            assertFalse(hasComponentOfType(postView, ComboBox.class), "Should NOT have posts dropdown in in-progress view");
        }
    }

    @Test
    void testNoInProgressPost_CompletedViewOnly() {
        // Arrange: No in-progress post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: Should be in completed view with dropdown, pagination, and start button
            assertTrue(postView.showingCompletedPosts, "Should be showing completed posts view");
            assertTrue(hasComponent(postView, "Start New Post"), "Should have 'Start New Post' button");
            assertTrue(hasComponentOfType(postView, ComboBox.class), "Should have posts dropdown");
            assertTrue(hasPaginationControls(postView), "Should have pagination controls");
            assertFalse(hasComponent(postView, "Switch to completed posts"), "Should NOT have 'Switch to completed posts' button");
            assertFalse(hasComponent(postView, "Switch to in-progress post"), "Should NOT have 'Switch to in-progress post' button");
            assertFalse(hasComponent(postView, "Finish Post"), "Should NOT have 'Finish Post' button");
        }
    }

    @Test
    void testCannotShowInProgressViewWhenNoInProgressPost() {
        // Arrange: No in-progress post exists
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: Cannot switch to in-progress view (no switch button available)
            assertFalse(hasComponent(postView, "Switch to in-progress post"), 
                       "Should NOT have 'Switch to in-progress post' button when no in-progress post exists");
            
            // Try to manually set to in-progress view (should not be possible through UI)
            postView.showingCompletedPosts = false;
            invokeMethod(postView, "recreateHeader");
            
            // Should still show completed posts layout since no in-progress post exists
            assertTrue(hasComponentOfType(postView, ComboBox.class), "Should still show posts dropdown");
            assertTrue(hasPaginationControls(postView), "Should still show pagination controls");
            assertFalse(hasComponent(postView, "Finish Post"), "Should NOT have 'Finish Post' button");
        }
    }

    @Test
    void testSwitchBetweenViews() {
        // Arrange: Start with in-progress post and in-progress view
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Initial state: in-progress view
            assertFalse(postView.showingCompletedPosts, "Should start in in-progress view");
            assertTrue(hasComponent(postView, "Switch to completed posts"), "Should have switch button");

            // Act: Switch to completed posts view
            Button switchButton = findComponent(postView, Button.class, "Switch to completed posts");
            assertNotNull(switchButton, "Switch button should exist");
            switchButton.click();

            // Assert: Now in completed view
            assertTrue(postView.showingCompletedPosts, "Should now be in completed posts view");
            assertTrue(hasComponent(postView, "Switch to in-progress post"), "Should have switch back button");
            assertTrue(hasComponentOfType(postView, ComboBox.class), "Should have dropdown");
            assertFalse(hasComponent(postView, "Finish Post"), "Should NOT have finish button");

            // Act: Switch back to in-progress view
            Button switchBackButton = findComponent(postView, Button.class, "Switch to in-progress post");
            assertNotNull(switchBackButton, "Switch back button should exist");
            switchBackButton.click();

            // Assert: Back to in-progress view
            assertFalse(postView.showingCompletedPosts, "Should be back in in-progress view");
            assertTrue(hasComponent(postView, "Switch to completed posts"), "Should have switch button again");
            assertTrue(hasComponent(postView, "Finish Post"), "Should have finish button again");
        }
    }

    @Test
    void testTimezoneDifferentFromSystemDefault() {
        // Arrange: Set up a timezone different from system default
        setupUIMocks("Europe/Berlin"); // Different timezone from the default NY
        
        // Create a post with a known UTC time for testing
        Date knownUtcTime = new Date(1640995200000L); // 2022-01-01 00:00:00 UTC
        
        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            // Act: Test the DateTimeUtils.formatDateTime method directly
            String formattedTime = DateTimeUtils.formatDateTime(knownUtcTime);
            
            // Assert: Time should be formatted in Berlin timezone (UTC+1)
            // Jan 01, 2022 at 1:00 AM (Berlin is UTC+1)
            assertTrue(formattedTime.contains("Jan 01, 2022"), 
                      "Should show the correct date in Berlin timezone");
            assertTrue(formattedTime.contains("1:00"), 
                      "Should show the correct hour in Berlin timezone (UTC+1)"); 
        }
    }

    // Helper methods for component discovery and assertions

    private boolean hasComponent(Component parent, String text) {
        return findComponent(parent, Button.class, text) != null;
    }

    private boolean hasComponentOfType(Component parent, Class<?> componentType) {
        return findComponentOfTypeGeneric(parent, componentType) != null;
    }

    @SuppressWarnings("unchecked")
    private Component findComponentOfTypeGeneric(Component parent, Class<?> componentType) {
        if (componentType.isInstance(parent)) {
            return parent;
        }

        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            Component result = findComponentOfTypeGeneric(child, componentType);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean hasPaginationControls(Component parent) {
        // Check for pagination-specific components (page navigation buttons, etc.)
        return hasClassInHierarchy(parent, "pagination-controls") || 
               hasClassInHierarchy(parent, "pagination-row");
    }

    private boolean hasPostStartedDate(Component parent) {
        // Look for the date span that shows "Post started ..."
        return findComponentWithText(parent, Span.class, "Post started") != null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponent(Component parent, Class<T> componentType, String text) {
        if (componentType.isInstance(parent)) {
            T component = (T) parent;
            if (component instanceof Button && text.equals(((Button) component).getText())) {
                return component;
            }
        }

        if (parent instanceof VerticalLayout || parent instanceof com.vaadin.flow.component.orderedlayout.HorizontalLayout) {
            for (Component child : parent.getChildren().toArray(Component[]::new)) {
                T result = findComponent(child, componentType, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentOfType(Component parent, Class<T> componentType) {
        if (componentType.isInstance(parent)) {
            return (T) parent;
        }

        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            T result = findComponentOfType(child, componentType);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentWithText(Component parent, Class<T> componentType, String textContains) {
        if (componentType.isInstance(parent)) {
            T component = (T) parent;
            if (component instanceof Span) {
                String text = ((Span) component).getText();
                if (text != null && text.contains(textContains)) {
                    return component;
                }
            }
        }

        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            T result = findComponentWithText(child, componentType, textContains);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean hasClassInHierarchy(Component parent, String className) {
        if (parent.getElement().getAttribute("class") != null && 
            parent.getElement().getAttribute("class").contains(className)) {
            return true;
        }

        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            if (hasClassInHierarchy(child, className)) {
                return true;
            }
        }
        return false;
    }

    private void invokeMethod(Object target, String methodName) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }

    private void setupUIMocks(String timeZoneId) {
        // Setup the mock chain: UI.getCurrent().getSession().getAttribute(ExtendedClientDetails.class)
        lenient().when(mockUI.getSession()).thenReturn(mockSession);
        lenient().when(mockExtendedClientDetails.getTimeZoneId()).thenReturn(timeZoneId);
        lenient().when(mockSession.getAttribute(ExtendedClientDetails.class)).thenReturn(mockExtendedClientDetails);
    }
    

    private com.vaadin.flow.router.AfterNavigationEvent mockAfterNavigationEvent() {
        com.vaadin.flow.router.AfterNavigationEvent event = mock(com.vaadin.flow.router.AfterNavigationEvent.class);
        com.vaadin.flow.router.Location location = mock(com.vaadin.flow.router.Location.class);
        return event;
    }
}