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
class GTGViewTest {

    @Mock
    private CallChainPresenter callChainPresenter;

    private GTGView gtgView;
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
    void testGTGViewCreation() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
        assertTrue(gtgView.hasClassName("gtg-view"));
    }

    @Test
    void testGTGViewWithNoData() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testGTGViewWithEmptyPairs() {
        // Arrange
        testGoToGuySet.setGoToGuyPairs(Collections.emptyList());
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testGTGViewLayoutProperties() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertTrue(gtgView.isPadding());
        assertTrue(gtgView.isSpacing());
        assertTrue(gtgView.hasClassName("gtg-view"));
    }

    @Test
    void testGTGViewContainsExpectedComponents() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        // Check that view contains expected component types
        boolean hasHeader = gtgView.getChildren()
            .anyMatch(component -> component instanceof H2);
        assertTrue(hasHeader, "GTG view should have a header");

        boolean hasGrid = gtgView.getChildren()
            .anyMatch(component -> component instanceof Grid);
        assertTrue(hasGrid, "GTG view should have a grid");
    }

    @Test
    void testGTGViewWithUsersHavingProfileImages() {
        // Arrange
        user1.setProfileImage("user1.jpg");
        user2.setProfileImage("user2.jpg");
        lenient().when(callChainPresenter.getFileUrl("user1.jpg")).thenReturn("http://example.com/user1.jpg");
        lenient().when(callChainPresenter.getFileUrl("user2.jpg")).thenReturn("http://example.com/user2.jpg");
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testGTGViewWithUsersWithoutPhoneNumbers() {
        // Arrange
        user1.setPhoneNumber(null);
        user2.setPhoneNumber(null);
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testGTGViewHandlesFileStorageServiceFailure() {
        // Arrange
        user1.setProfileImage("user1.jpg");
        lenient().when(callChainPresenter.getFileUrl("user1.jpg")).thenThrow(new RuntimeException("Storage error"));
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act & Assert
        assertDoesNotThrow(() -> {
            gtgView = new GTGView(callChainPresenter);
        });
    }

    @Test
    void testGTGViewConstructorWithNullPresenter() {
        // Act & Assert
        // GTGView constructor may not validate null parameters, so just test it doesn't crash
        assertDoesNotThrow(() -> {
            GTGView view = new GTGView(null);
            assertNotNull(view);
        });
    }

    @Test
    void testGTGViewConstructorWithNullFileService() {
        // Arrange
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
    }

    @Test
    void testGTGViewWithSinglePair() {
        // Arrange
        testGoToGuySet.setGoToGuyPairs(Arrays.asList(pair1));
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testGTGViewWithUsersMissingNames() {
        // Arrange
        user1.setFirstName(null);
        user1.setLastName(null);
        user2.setFirstName("");
        user2.setLastName("");
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);

        // Act
        gtgView = new GTGView(callChainPresenter);

        // Assert
        assertNotNull(gtgView);
        assertTrue(gtgView.getChildren().count() > 0);
    }

    @Test
    void testFormatPhoneNumberVariantsViaReflection() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        gtgView = new GTGView(callChainPresenter);

        Method formatPhoneNumber = GTGView.class.getDeclaredMethod("formatPhoneNumber", String.class);
        formatPhoneNumber.setAccessible(true);

        assertEquals("(555) 111-2222", formatPhoneNumber.invoke(gtgView, "5551112222"));
        assertEquals("(555) 111-2222", formatPhoneNumber.invoke(gtgView, "15551112222"));
        assertEquals("12", formatPhoneNumber.invoke(gtgView, "12"));
        assertEquals("No phone", formatPhoneNumber.invoke(gtgView, ""));
        assertEquals("No phone", formatPhoneNumber.invoke(gtgView, new Object[]{null}));
    }

    @Test
    void testLoadDataHandlesNullStartDateAndExceptions() {
        // Null start date branch
        testGoToGuySet.setStartDate(null);
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        assertDoesNotThrow(() -> new GTGView(callChainPresenter));

        // Exception branch
        when(callChainPresenter.getCurrentGoToGuySet()).thenThrow(new RuntimeException("boom"));
        assertDoesNotThrow(() -> new GTGView(callChainPresenter));
    }

    @Test
    void testCreateUserComponentBranchesViaReflection() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(testGoToGuySet);
        when(callChainPresenter.getFileUrl("avatar.jpg")).thenReturn("http://cdn/avatar.jpg");
        gtgView = new GTGView(callChainPresenter);

        Method createUserComponent = GTGView.class.getDeclaredMethod("createUserComponent", User.class);
        createUserComponent.setAccessible(true);

        User withImage = new User();
        withImage.setFirstName("A");
        withImage.setPhoneNumber("5551112222");
        withImage.setProfileImage("avatar.jpg");
        HorizontalLayout withImageLayout = (HorizontalLayout) createUserComponent.invoke(gtgView, withImage);
        assertTrue(withImageLayout.getChildren().count() >= 2);

        User withoutImage = new User();
        withoutImage.setEmail("fallback@example.com");
        withoutImage.setPhoneNumber(null);
        HorizontalLayout withoutImageLayout = (HorizontalLayout) createUserComponent.invoke(gtgView, withoutImage);
        assertTrue(withoutImageLayout.getChildren().count() >= 2);
    }
}
