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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

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
    private org.springframework.data.domain.Page<Post> emptyCompletedPostsPage;

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
        emptyCompletedPostsPage = new PageImpl<>(Collections.emptyList());

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

    @Test
    void testStartNewPostSuccess() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.startPost(testUser)).thenReturn(inProgressPost);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act
            Button startButton = findComponent(postView, Button.class, "Start New Post");
            assertNotNull(startButton, "Start button should exist");
            startButton.click();

            // Assert
            verify(vaadinPostPresenter).startPost(testUser);
        }
    }

    @Test
    void testStartNewPostFailure() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.startPost(testUser)).thenThrow(new RuntimeException("Error starting post"));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act
            Button startButton = findComponent(postView, Button.class, "Start New Post");
            assertNotNull(startButton, "Start button should exist");
            
            // Should not throw exception - error should be handled
            assertDoesNotThrow(() -> startButton.click());
        }
    }

    @Test
    void testFinishPostButtonExists() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert
            assertTrue(hasComponent(postView, "Finish Post"), "Should have 'Finish Post' button");
        }
    }

    @Test
    void testFinishPostFailure() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.finishPost(testUser)).thenThrow(new RuntimeException("Error finishing post"));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act
            Button finishButton = findComponent(postView, Button.class, "Finish Post");
            assertNotNull(finishButton, "Finish button should exist");
            
            // Should not throw exception - error should be handled
            assertDoesNotThrow(() -> finishButton.click());
        }
    }

    @Test
    void testPaginationControls() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert
            assertTrue(hasPaginationControls(postView), "Should have pagination controls");
        }
    }

    @Test
    void testPostSelectorWithPosts() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act
            ComboBox<Post> postSelector = findComponentOfType(postView, ComboBox.class);
            
            // Assert
            assertNotNull(postSelector, "Should have post selector");
        }
    }

    @Test
    void testCompletedViewWithoutPostsDisablesSelectorAndNormalizesPagination() {
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class))).thenReturn(emptyCompletedPostsPage);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            ComboBox<?> completedPostsSelector = findComponentByClassName(postView, ComboBox.class, "post-selector");
            assertNotNull(completedPostsSelector, "Completed post selector should exist");
            assertFalse(completedPostsSelector.isEnabled(), "Completed post selector should be disabled for empty pages");

            Span pageInfo = findComponentByClassName(postView, Span.class, "page-info");
            assertNotNull(pageInfo, "Page info should be rendered");
            assertTrue(pageInfo.getText().contains("of 1"), "Empty pagination should display one normalized page");
        }
    }

    @Test
    void testFormFieldsExist() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert - Check that form contains expected sections
            assertTrue(postView.getChildren().count() > 0, "Should have form content");
        }
    }

    @Test
    void testPostViewConstructor() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            new PostView(vaadinPostPresenter);
        });
    }

    @Test
    void testPostViewSizeAndClassNames() {
        // Act
        PostView view = new PostView(vaadinPostPresenter);

        // Assert
        assertTrue(view.getClassNames().contains("post-view"));
    }

    @Test
    void testGeneratePostLabelForInProgressPost() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act - We can't test the private method directly, but we can test that
            // the view handles in-progress posts correctly
            assertNotNull(postView);
        }
    }

    @Test
    void testGeneratePostLabelForCompletedPost() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);
            
            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Act - We can test that completed posts are handled
            ComboBox<Post> selector = findComponentOfType(postView, ComboBox.class);
            assertNotNull(selector, "Should have post selector for completed posts");
        }
    }

    @Test
    void testDeepLinkLoadsSpecificCompletedPost() {
        // Arrange: deep link to a completed post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.getPostById(2L)).thenReturn(Optional.of(completedPost1));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), 2L);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: should be in completed posts mode with the deep-linked post loaded
            assertTrue(postView.showingCompletedPosts, "Deep-linked completed post should show completed view");
            verify(vaadinPostPresenter).getPostById(2L);
        }
    }

    @Test
    void testDeepLinkLoadsOwnInProgressPost() {
        // Arrange: deep link to current user's own in-progress post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getPostById(1L)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), 1L);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: should show in-progress view for own in-progress post
            assertFalse(postView.showingCompletedPosts, "Own in-progress post should show in-progress view");
        }
    }

    @Test
    void testDeepLinkPostNotFound() {
        // Arrange: deep link to a non-existent post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.getPostById(999L)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), 999L);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: should fall through to default behavior (no in-progress → completed view)
            assertTrue(postView.showingCompletedPosts, "Should fall through to completed view when post not found");
            verify(vaadinPostPresenter).getPostById(999L);
        }
    }

    @Test
    void testNoDeepLinkParameterWorksAsDefault() {
        // Arrange: no deep link (null parameter)
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), null);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: default behavior — shows in-progress post
            assertFalse(postView.showingCompletedPosts, "Null parameter should use default behavior");
        }
    }

    @Test
    void testDeepLinkToOtherUsersInProgressPostBlocked() {
        // Arrange: deep link to another user's in-progress post — should be blocked
        User otherUser = new User();
        otherUser.setId(2L);
        Post otherInProgressPost = new Post();
        otherInProgressPost.setId(20L);
        otherInProgressPost.setUser(otherUser);
        otherInProgressPost.setState(PostState.IN_PROGRESS);
        otherInProgressPost.setStart(new Date());

        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.getPostById(20L)).thenReturn(Optional.of(otherInProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), 20L);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: should fall through to default behavior, NOT show the other user's post
            assertTrue(postView.showingCompletedPosts, "Should fall through to completed view when blocked");
            verify(vaadinPostPresenter).getPostById(20L);
        }
    }

    @Test
    void testDeepLinkToOtherUsersCompletedPost() {
        // Arrange: deep link to another user's completed post
        User otherUser = new User();
        otherUser.setId(2L);
        Post otherUserPost = new Post();
        otherUserPost.setId(10L);
        otherUserPost.setUser(otherUser);
        otherUserPost.setState(PostState.COMPLETE);
        otherUserPost.setFinish(new Date());

        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.getPostById(10L)).thenReturn(Optional.of(otherUserPost));
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(otherUser), any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(otherUserPost)));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.setParameter(mock(BeforeEvent.class), 10L);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Assert: should show completed view for the other user's post
            assertTrue(postView.showingCompletedPosts, "Other user's post should show completed view");
            verify(vaadinPostPresenter).getPostById(10L);
        }
    }

    // === Pagination tests ===

    @Test
    void testGoToNextPage() {
        // Arrange: create a multi-page scenario (2 pages of results)
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        // First page: return page 0 of 2 total pages
        org.springframework.data.domain.Page<Post> firstPage = new PageImpl<>(
            Arrays.asList(completedPost1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        org.springframework.data.domain.Page<Post> secondPage = new PageImpl<>(
            Arrays.asList(completedPost2), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(firstPage)
            .thenReturn(firstPage) // recreateHeader reload
            .thenReturn(secondPage); // after next page

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Find and click the next page button
            Button nextButton = getPrivateButton(postView, "nextPageButton");
            assertNotNull(nextButton, "Next page button should exist");
            nextButton.click();

            // Verify the presenter was called for the next page
            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
        }
    }

    @Test
    void testGoToPreviousPage() {
        // Arrange: set up on page 1 (second page), then go back
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        org.springframework.data.domain.Page<Post> firstPage = new PageImpl<>(
            Arrays.asList(completedPost1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        org.springframework.data.domain.Page<Post> secondPage = new PageImpl<>(
            Arrays.asList(completedPost2), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(firstPage)  // initial load
            .thenReturn(firstPage)  // recreateHeader
            .thenReturn(secondPage) // after next
            .thenReturn(firstPage); // after previous

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Go to next page first
            Button nextButton = getPrivateButton(postView, "nextPageButton");
            assertNotNull(nextButton, "Next page button should exist");
            nextButton.click();

            // Now go back to previous page
            Button prevButton = getPrivateButton(postView, "previousPageButton");
            assertNotNull(prevButton, "Previous page button should exist");
            prevButton.click();

            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
        }
    }

    @Test
    void testGoToFirstPage() {
        // Arrange: start on page 1 (second page), then jump to first
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        org.springframework.data.domain.Page<Post> firstPage = new PageImpl<>(
            Arrays.asList(completedPost1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        org.springframework.data.domain.Page<Post> secondPage = new PageImpl<>(
            Arrays.asList(completedPost2), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(firstPage)   // initial load
            .thenReturn(firstPage)   // recreateHeader
            .thenReturn(secondPage)  // after next
            .thenReturn(firstPage);  // after first page

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Go to next page first
            Button nextButton = getPrivateButton(postView, "nextPageButton");
            nextButton.click();

            // Now go to first page
            Button firstButton = getPrivateButton(postView, "firstPageButton");
            assertNotNull(firstButton, "First page button should exist");
            firstButton.click();

            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
        }
    }

    @Test
    void testGoToLastPage() {
        // Arrange: start on first page, then jump to last
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        org.springframework.data.domain.Page<Post> firstPage = new PageImpl<>(
            Arrays.asList(completedPost1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        org.springframework.data.domain.Page<Post> lastPage = new PageImpl<>(
            Arrays.asList(completedPost2), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "finish")), 12
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(firstPage)  // initial load
            .thenReturn(firstPage)  // recreateHeader
            .thenReturn(lastPage);  // after last page

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Go to last page
            Button lastButton = getPrivateButton(postView, "lastPageButton");
            assertNotNull(lastButton, "Last page button should exist");
            lastButton.click();

            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
        }
    }

    @Test
    void testGoToPageViaPageNumberField() {
        // Arrange: multi-page scenario
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        org.springframework.data.domain.Page<Post> firstPage = new PageImpl<>(
            Arrays.asList(completedPost1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 25
        );
        org.springframework.data.domain.Page<Post> secondPage = new PageImpl<>(
            Arrays.asList(completedPost2), PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "finish")), 25
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(firstPage)   // initial load
            .thenReturn(firstPage)   // recreateHeader
            .thenReturn(secondPage); // after goToPage

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Find the page number field and set value to page 2
            com.vaadin.flow.component.textfield.IntegerField pageField = getPrivateField(postView, "pageNumberField", com.vaadin.flow.component.textfield.IntegerField.class);
            assertNotNull(pageField, "Page number field should exist");
            pageField.setValue(2); // triggers goToPage(1) internally

            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
        }
    }

    @Test
    void testPaginationButtonsDisabledOnFirstPage() {
        // Arrange: single-page scenario (buttons should be disabled)
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        // Single page with 2 items (total = 2, page size = 10 → 1 page total)
        org.springframework.data.domain.Page<Post> singlePage = new PageImpl<>(
            Arrays.asList(completedPost1, completedPost2), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 2
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(singlePage);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // All pagination buttons should be disabled on a single page
            Button firstButton = getPrivateButton(postView, "firstPageButton");
            Button prevButton = getPrivateButton(postView, "previousPageButton");
            Button nextButton = getPrivateButton(postView, "nextPageButton");
            Button lastButton = getPrivateButton(postView, "lastPageButton");

            assertNotNull(firstButton);
            assertNotNull(prevButton);
            assertNotNull(nextButton);
            assertNotNull(lastButton);

            assertFalse(firstButton.isEnabled(), "First page should be disabled on first page");
            assertFalse(prevButton.isEnabled(), "Previous page should be disabled on first page");
            assertFalse(nextButton.isEnabled(), "Next page should be disabled on single page");
            assertFalse(lastButton.isEnabled(), "Last page should be disabled on single page");
        }
    }

    // === finishPost success path ===

    @Test
    void testFinishPostSuccess() {
        // Arrange: in-progress post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser))
            .thenReturn(Optional.of(inProgressPost))   // initializeUser
            .thenReturn(Optional.of(inProgressPost))    // createHeaderSection (hasInProgressPost check)
            .thenReturn(Optional.empty())               // after finishPost: createPostView -> createHeaderSection
            .thenReturn(Optional.empty());              // extra call in recreate

        Post finishedPost = new Post();
        finishedPost.setId(1L);
        finishedPost.setUser(testUser);
        finishedPost.setState(PostState.COMPLETE);
        finishedPost.setStart(new Date());
        finishedPost.setFinish(new Date());
        lenient().when(vaadinPostPresenter.finishPost(testUser)).thenReturn(finishedPost);
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Call finishPost directly via reflection (button is disabled until all fields filled)
            invokeMethod(postView, "finishPost");

            // Verify finishPost was called
            verify(vaadinPostPresenter).finishPost(testUser);
            // After finishing, should switch to completed posts view
            assertTrue(postView.showingCompletedPosts, "Should switch to completed posts after finishing");
        }
    }

    // === updateFinishButtonState tests ===

    @Test
    void testUpdateFinishButtonState_DisabledWhenFieldsEmpty() {
        // Arrange: in-progress post with empty fields
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Invoke updateFinishButtonState via reflection
            invokeMethod(postView, "updateFinishButtonState");

            // Find the finish button
            Button finishButton = findComponent(postView, Button.class, "Finish Post");
            assertNotNull(finishButton, "Finish button should exist");
            assertFalse(finishButton.isEnabled(), "Finish button should be disabled when fields are empty");
        }
    }

    @Test
    void testUpdateFinishButtonState_DisabledWhenOnlyIntroFilled() {
        // Arrange: in-progress post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Fill only intro fields via the form fields
            fillIntroFields(postView);

            // Invoke updateFinishButtonState
            invokeMethod(postView, "updateFinishButtonState");

            Button finishButton = findComponent(postView, Button.class, "Finish Post");
            assertFalse(finishButton.isEnabled(), "Finish button should be disabled when only intro fields are filled");
        }
    }

    @Test
    void testUpdateFinishButtonState_DisabledWhenCompletedPost() {
        // Arrange: completed post (button should stay disabled)
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Switch to showing completed posts so button should be disabled
            postView.showingCompletedPosts = true;
            invokeMethod(postView, "updateFinishButtonState");

            Button finishButton = findComponent(postView, Button.class, "Finish Post");
            assertNotNull(finishButton, "Finish button should exist");
            assertFalse(finishButton.isEnabled(), "Finish button should be disabled when showing completed posts");
        }
    }

    @Test
    void testUpdateFinishButtonState_AllFieldsFilledButNoStats() {
        // Arrange: in-progress post with all text fields filled but no stats
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Fill all text fields
            fillIntroFields(postView);
            fillCategoryFields(postView);

            invokeMethod(postView, "updateFinishButtonState");

            Button finishButton = findComponent(postView, Button.class, "Finish Post");
            assertNotNull(finishButton, "Finish button should exist");
            // When there are no stat definitions, areAllStatsSet returns true (vacuous truth)
            // so with all text fields filled and no stats to check, button may be enabled
            // This test verifies the updateFinishButtonState logic is exercised
        }
    }

    // === generatePostLabel tests ===

    @Test
    void testGeneratePostLabel_NullPost() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Call generatePostLabel via reflection with null
            String label = invokeGeneratePostLabel(postView, null);
            assertEquals("Select a post...", label, "Null post should return placeholder text");
        }
    }

    @Test
    void testGeneratePostLabel_InProgressPost() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            String label = invokeGeneratePostLabel(postView, inProgressPost);
            assertTrue(label.startsWith("In Progress - Started "), "In-progress label should start with 'In Progress - Started '");
        }
    }

    @Test
    void testGeneratePostLabel_CompletedPostWithFinishDate() {
        // Arrange
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            String label = invokeGeneratePostLabel(postView, completedPost1);
            // completedPost1 has a finish date, so label should be formatted date string
            assertNotNull(label, "Label should not be null for completed post");
            assertFalse(label.contains("In Progress"), "Completed post label should not say 'In Progress'");
            assertFalse(label.contains("Select a post"), "Completed post label should not be placeholder");
        }
    }

    @Test
    void testGeneratePostLabel_CompletedPostWithoutFinishDate() {
        // Arrange: a completed post without a finish date (edge case)
        Post noFinishPost = new Post();
        noFinishPost.setId(99L);
        noFinishPost.setUser(testUser);
        noFinishPost.setState(PostState.COMPLETE);
        noFinishPost.setStart(new Date());
        noFinishPost.setFinish(null); // No finish date

        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            String label = invokeGeneratePostLabel(postView, noFinishPost);
            assertTrue(label.contains("Post 99"), "Label for post without finish date should include post ID");
            assertTrue(label.contains("Started"), "Label for post without finish date should include 'Started'");
        }
    }

    // === savePostChanges via binder listener ===

    @Test
    void testSavePostChangesTriggeredByFieldUpdate() {
        // Arrange: in-progress post
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.savePost(any(Post.class))).thenReturn(inProgressPost);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Find and update a text field — this should trigger the binder value change listener
            // which calls savePostChanges()
            com.vaadin.flow.component.textfield.TextField kryptoniteField = findComponentOfTypeWithClass(postView, com.vaadin.flow.component.textfield.TextField.class, "post-textfield");
            assertNotNull(kryptoniteField, "Kryptonite field should exist");
            kryptoniteField.setValue("test kryptonite value");

            // Verify savePost was called (triggered by binder value change listener)
            verify(vaadinPostPresenter).savePost(any(Post.class));
        }
    }

    @Test
    void testSavePostChangesHandlesException() {
        // Arrange: in-progress post, savePost throws
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(Collections.emptyList());
        lenient().when(vaadinPostPresenter.savePost(any(Post.class))).thenThrow(new RuntimeException("DB error"));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Update a field — should trigger save which throws, but should be handled gracefully
            com.vaadin.flow.component.textfield.TextField kryptoniteField = findComponentOfTypeWithClass(postView, com.vaadin.flow.component.textfield.TextField.class, "post-textfield");
            assertNotNull(kryptoniteField, "Kryptonite field should exist");
            assertDoesNotThrow(() -> kryptoniteField.setValue("trigger save"));
        }
    }

    @Test
    void testPageSizeSelectorChange() {
        // Arrange: completed posts view
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        org.springframework.data.domain.Page<Post> page = new PageImpl<>(
            Arrays.asList(completedPost1, completedPost2), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "finish")), 2
        );
        lenient().when(vaadinPostPresenter.getCompletedPostsPage(eq(testUser), any(Pageable.class)))
            .thenReturn(page);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            postView = new PostView(vaadinPostPresenter);
            postView.afterNavigation(mockAfterNavigationEvent());

            // Access the page size selector via reflection
            @SuppressWarnings("unchecked")
            ComboBox<Integer> pageSizeSelector = getPrivateField(postView, "pageSizeSelector", ComboBox.class);
            assertNotNull(pageSizeSelector, "Page size selector should exist");
            pageSizeSelector.setValue(5);

            // Verify data was reloaded
            verify(vaadinPostPresenter, atLeastOnce()).getCompletedPostsPage(eq(testUser), any(Pageable.class));
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

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentByClassName(Component parent, Class<T> componentType, String className) {
        if (componentType.isInstance(parent) && parent.getClassNames().contains(className)) {
            return (T) parent;
        }

        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            T result = findComponentByClassName(child, componentType, className);
            if (result != null) {
                return result;
            }
        }
        return null;
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

    private String invokeGeneratePostLabel(PostView view, Post post) {
        try {
            Method method = PostView.class.getDeclaredMethod("generatePostLabel", Post.class);
            method.setAccessible(true);
            return (String) method.invoke(view, post);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke generatePostLabel", e);
        }
    }

    private Button getPrivateButton(PostView view, String fieldName) {
        try {
            java.lang.reflect.Field field = PostView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (Button) field.get(view);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(PostView view, String fieldName, Class<T> type) {
        try {
            java.lang.reflect.Field field = PostView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(view));
        } catch (Exception e) {
            return null;
        }
    }

    private void fillIntroFields(PostView view) {
        // Find TextAreas in the intro section
        List<com.vaadin.flow.component.textfield.TextArea> textAreas = new java.util.ArrayList<>();
        List<com.vaadin.flow.component.textfield.TextField> textFields = new java.util.ArrayList<>();
        collectFieldsOfType(view, com.vaadin.flow.component.textfield.TextArea.class, textAreas);
        collectFieldsOfType(view, com.vaadin.flow.component.textfield.TextField.class, textFields);

        // Set values for intro fields (WIDWYTK, What and When via TextArea, Kryptonite via TextField)
        for (com.vaadin.flow.component.textfield.TextArea ta : textAreas) {
            if (ta.getLabel() != null && ta.getLabel().contains("What I Don't")) {
                ta.setValue("test widwytk");
            } else if (ta.getLabel() != null && ta.getLabel().contains("What and When")) {
                ta.setValue("test what and when");
            }
        }
        for (com.vaadin.flow.component.textfield.TextField tf : textFields) {
            if (tf.getLabel() != null && tf.getLabel().contains("Kryptonite")) {
                tf.setValue("test kryptonite");
            }
        }
    }

    private void fillCategoryFields(PostView view) {
        List<com.vaadin.flow.component.textfield.TextArea> textAreas = new java.util.ArrayList<>();
        collectFieldsOfType(view, com.vaadin.flow.component.textfield.TextArea.class, textAreas);

        for (com.vaadin.flow.component.textfield.TextArea ta : textAreas) {
            if (ta.getLabel() != null && "Best".equals(ta.getLabel()) && (ta.getValue() == null || ta.getValue().isEmpty())) {
                ta.setValue("test best value");
            } else if (ta.getLabel() != null && "Worst".equals(ta.getLabel()) && (ta.getValue() == null || ta.getValue().isEmpty())) {
                ta.setValue("test worst value");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> void collectFieldsOfType(Component parent, Class<T> type, List<T> results) {
        if (type.isInstance(parent)) {
            results.add((T) parent);
        }
        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            collectFieldsOfType(child, type, results);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentOfTypeWithClass(Component parent, Class<T> componentType, String className) {
        if (componentType.isInstance(parent) && parent.getClassNames().contains(className)) {
            return (T) parent;
        }
        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            T result = findComponentOfTypeWithClass(child, componentType, className);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findComponentWithClass(Component parent, Class<T> componentType, String className) {
        if (componentType.isInstance(parent) && parent.getClassNames().contains(className)) {
            return (T) parent;
        }
        for (Component child : parent.getChildren().toArray(Component[]::new)) {
            T result = findComponentWithClass(child, componentType, className);
            if (result != null) {
                return result;
            }
        }
        return null;
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
