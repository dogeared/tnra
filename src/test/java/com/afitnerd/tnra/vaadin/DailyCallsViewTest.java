package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyCallsViewTest {

    @Mock
    private CallChainPresenter callChainPresenter;

    private DailyCallsView dailyCallsView;
    private GoToGuySet testGoToGuySet;
    private User user1, user2, user3;
    private GoToGuyPair pair1, pair2;

    @BeforeEach
    void setUp() {
        // Create test users
        user1 = new User();
        user1.setId(1L);
        user1.setSlackUserId("user1");
        user1.setSlackUsername("user1");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setPhoneNumber("555-1234");

        user2 = new User();
        user2.setId(2L);
        user2.setSlackUserId("user2");
        user2.setSlackUsername("user2");
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setPhoneNumber("555-5678");

        user3 = new User();
        user3.setId(3L);
        user3.setSlackUserId("user3");
        user3.setSlackUsername("user3");
        user3.setFirstName("Bob");
        user3.setLastName("Johnson");
        user3.setPhoneNumber("555-9012");

        // Create test Go To Guy pairs
        pair1 = new GoToGuyPair();
        pair1.setId(1L);
        pair1.setCaller(user1);
        pair1.setCallee(user2);

        pair2 = new GoToGuyPair();
        pair2.setId(2L);
        pair2.setCaller(user2);
        pair2.setCallee(user3);

        // Create test Go To Guy set
        testGoToGuySet = new GoToGuySet();
        testGoToGuySet.setId(1L);
        testGoToGuySet.setGoToGuyPairs(Arrays.asList(pair1, pair2));
        testGoToGuySet.setStartDate(new java.util.Date());

        // Setup mocks
        lenient().when(callChainPresenter.getFileUrl(anyString())).thenReturn(null);
    }

    @Test
    void testDailyCallsViewCreation() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
        assertTrue(dailyCallsView.hasClassName("gtg-view"));
    }

    @Test
    void testDailyCallsViewWithNoData() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testDailyCallsViewWithEmptyPairs() {
        // Arrange
        testGoToGuySet.setGoToGuyPairs(Collections.emptyList());
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testDailyCallsViewLayoutProperties() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertTrue(dailyCallsView.isPadding());
        assertTrue(dailyCallsView.isSpacing());
        assertTrue(dailyCallsView.hasClassName("gtg-view"));
    }

    @Test
    void testDailyCallsViewContainsExpectedComponents() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        // Check that view contains expected component types
        boolean hasHeader = dailyCallsView.getChildren()
            .anyMatch(component -> component instanceof H2);
        assertTrue(hasHeader, "GTG view should have a header");

        boolean hasGrid = dailyCallsView.getChildren()
            .anyMatch(component -> component instanceof Grid);
        assertTrue(hasGrid, "GTG view should have a grid");
    }

    @Test
    void testDailyCallsViewWithUsersHavingProfileImages() {
        // Arrange
        user1.setProfileImage("user1.jpg");
        user2.setProfileImage("user2.jpg");
        lenient().when(callChainPresenter.getFileUrl("user1.jpg")).thenReturn("http://example.com/user1.jpg");
        lenient().when(callChainPresenter.getFileUrl("user2.jpg")).thenReturn("http://example.com/user2.jpg");
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testDailyCallsViewWithUsersWithoutPhoneNumbers() {
        // Arrange
        user1.setPhoneNumber(null);
        user2.setPhoneNumber(null);
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testDailyCallsViewHandlesFileStorageServiceFailure() {
        // Arrange
        user1.setProfileImage("user1.jpg");
        lenient().when(callChainPresenter.getFileUrl("user1.jpg")).thenThrow(new RuntimeException("Storage error"));
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act & Assert
        assertDoesNotThrow(() -> {
            dailyCallsView = new DailyCallsView(callChainPresenter);
        });
    }

    @Test
    void testDailyCallsViewConstructorWithNullPresenter() {
        // Act & Assert
        // DailyCallsView constructor may not validate null parameters, so just test it doesn't crash
        assertDoesNotThrow(() -> {
            DailyCallsView view = new DailyCallsView(null);
            assertNotNull(view);
        });
    }

    @Test
    void testDailyCallsViewConstructorWithNullFileService() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
    }

    @Test
    void testDailyCallsViewWithSinglePair() {
        // Arrange
        testGoToGuySet.setGoToGuyPairs(Arrays.asList(pair1));
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testDailyCallsViewWithUsersMissingNames() {
        // Arrange
        user1.setFirstName(null);
        user1.setLastName(null);
        user2.setFirstName("");
        user2.setLastName("");
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        dailyCallsView = new DailyCallsView(callChainPresenter);

        // Assert
        assertNotNull(dailyCallsView);
        assertTrue(dailyCallsView.getChildren().count() > 0);
    }

    @Test
    void testFormatPhoneNumberVariantsViaReflection() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        dailyCallsView = new DailyCallsView(callChainPresenter);

        Method formatPhoneNumber = DailyCallsView.class.getDeclaredMethod("formatPhoneNumber", String.class);
        formatPhoneNumber.setAccessible(true);

        assertEquals("(555) 111-2222", formatPhoneNumber.invoke(dailyCallsView, "5551112222"));
        assertEquals("(555) 111-2222", formatPhoneNumber.invoke(dailyCallsView, "15551112222"));
        assertEquals("12", formatPhoneNumber.invoke(dailyCallsView, "12"));
        assertEquals("No phone", formatPhoneNumber.invoke(dailyCallsView, ""));
        assertEquals("No phone", formatPhoneNumber.invoke(dailyCallsView, new Object[]{null}));
    }

    @Test
    void testLoadDataHandlesNullStartDateAndExceptions() {
        // Null start date branch
        testGoToGuySet.setStartDate(null);
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        assertDoesNotThrow(() -> new DailyCallsView(callChainPresenter));

        // Exception branch
        when(callChainPresenter.getCurrentGoToGuySet()).thenThrow(new RuntimeException("boom"));
        assertDoesNotThrow(() -> new DailyCallsView(callChainPresenter));
    }

    @Test
    void testCreateUserComponentBranchesViaReflection() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        when(callChainPresenter.getFileUrl("avatar.jpg")).thenReturn("http://cdn/avatar.jpg");
        dailyCallsView = new DailyCallsView(callChainPresenter);

        Method createUserComponent = DailyCallsView.class.getDeclaredMethod("createUserComponent", User.class);
        createUserComponent.setAccessible(true);

        User withImage = new User();
        withImage.setFirstName("A");
        withImage.setPhoneNumber("5551112222");
        withImage.setProfileImage("avatar.jpg");
        HorizontalLayout withImageLayout = (HorizontalLayout) createUserComponent.invoke(dailyCallsView, withImage);
        assertTrue(withImageLayout.getChildren().count() >= 2);

        User withoutImage = new User();
        withoutImage.setEmail("fallback@example.com");
        withoutImage.setPhoneNumber(null);
        HorizontalLayout withoutImageLayout = (HorizontalLayout) createUserComponent.invoke(dailyCallsView, withoutImage);
        assertTrue(withoutImageLayout.getChildren().count() >= 2);
    }
}
