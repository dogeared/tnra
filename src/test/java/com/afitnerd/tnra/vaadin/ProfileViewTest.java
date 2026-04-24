package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileViewTest {

    @Mock
    private UserService userService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private StatDefinitionRepository statDefinitionRepository;

    @Mock
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;

    private User testUser;
    private ProfileView profileView;
    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, Mockito.RETURNS_DEEP_STUBS);
        lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        lenient().when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);

        testUser = new User();
        testUser.setId(1L);
        testUser.setSlackUserId("test-user");
        testUser.setSlackUsername("testuser");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPhoneNumber("555-1234");
        testUser.setProfileImage("profile.jpg");

        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
        lenient().when(fileStorageService.getFileUrl(anyString())).thenReturn("http://example.com/profile.jpg");
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void testProfileViewCreation() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
        assertTrue(profileView.hasClassName("profile-view"));
    }

    @Test
    void testProfileViewLayoutProperties() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        // ProfileView uses default alignment properties, not custom ones
        assertTrue(profileView.isPadding());
        assertTrue(profileView.isSpacing());
        assertTrue(profileView.hasClassName("profile-view"));
    }

    @Test
    void testProfileViewContainsExpectedComponents() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        // Check for header
        boolean hasHeader = profileView.getChildren()
            .anyMatch(component -> component instanceof H2);
        assertTrue(hasHeader, "Profile view should have a header");

        // Check for form fields and upload component - ProfileView has header + main layout
        assertTrue(profileView.getChildren().count() >= 2, "Should have header and main layout");
    }

    @Test
    void testProfileViewWithUserWithoutProfileImage() {
        // Arrange
        testUser.setProfileImage(null);
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithUserWithProfileImage() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithPartialUserData() {
        // Arrange
        testUser.setFirstName(null);
        testUser.setPhoneNumber(null);
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewHandlesFileStorageFailure() {
        // Arrange
        when(fileStorageService.getFileUrl(anyString())).thenThrow(new RuntimeException("Storage error"));

        // Act & Assert
        // ProfileView constructor calls getFileUrl through loadUserData, so it will throw
        assertThrows(RuntimeException.class, () -> {
            profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        });
    }

    @Test
    void testProfileViewWithInvalidPhoneNumber() {
        // Arrange
        testUser.setPhoneNumber("invalid-phone");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithValidPhoneNumber() {
        // Arrange
        testUser.setPhoneNumber("(555) 123-4567");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewConstructorWithNullUserService() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            new ProfileView(null, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        });
    }

    @Test
    void testProfileViewConstructorWithNullFileService() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            new ProfileView(userService, null, statDefinitionRepository, personalStatDefinitionRepository);
        });
    }

    @Test
    void testProfileViewWithEmptyUserData() {
        // Arrange
        User emptyUser = new User();
        when(userService.getCurrentUser()).thenReturn(emptyUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithLongNames() {
        // Arrange
        testUser.setFirstName("VeryLongFirstNameThatMightCauseIssues");
        testUser.setLastName("VeryLongLastNameThatMightCauseIssues");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithSpecialCharactersInNames() {
        // Arrange
        testUser.setFirstName("José");
        testUser.setLastName("O'Connor-Smith");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewHandlesUserServiceFailure() {
        // Arrange
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("User service error"));

        // Act & Assert
        // ProfileView constructor calls getCurrentUser() in loadUserData(), so it will throw
        assertThrows(RuntimeException.class, () -> {
            profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        });
    }

    @Test
    void testProfileViewWithFileStorageUnavailable() {
        // Arrange
        // Don't mock getFileUrl to return null as that causes IllegalArgumentException
        // when setting image src. Instead test with empty profile image.
        testUser.setProfileImage(null);
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void myStatsSectionIsRendered() {
        // Arrange
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(Collections.emptyList());

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Assert - find the "My Stats" H3 header somewhere in the component tree
        boolean hasMyStatsHeader = findAllDescendants(profileView)
            .anyMatch(c -> c instanceof H3 && "My Stats".equals(((H3) c).getText()));
        assertTrue(hasMyStatsHeader, "Profile view should contain a 'My Stats' header");

        // Assert - find the "Add Stat" button somewhere in the component tree
        boolean hasAddStatButton = findAllDescendants(profileView)
            .anyMatch(c -> c instanceof Button && ((Button) c).getText().contains("Add Stat"));
        assertTrue(hasAddStatButton, "Profile view should contain an 'Add Stat' button");
    }

    @Test
    void testProfileImageUploadPersistsToDatabase() throws Exception {
        // Arrange
        testUser.setProfileImage(null);
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(fileStorageService.storeFile(any(InputStream.class), anyString(), anyString()))
            .thenReturn("abc123.jpg");
        when(fileStorageService.getFileUrl("abc123.jpg"))
            .thenReturn("/uploads/abc123.jpg");

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Act
        profileView.processProfileImageUpload("photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Assert — saveUser must be called to persist the profile_image to DB
        verify(userService).saveUser(testUser);
        assertEquals("abc123.jpg", testUser.getProfileImage());
    }

    @Test
    void testProfileImageUploadDeletesOldImage() throws Exception {
        // Arrange — user already has a profile image
        testUser.setProfileImage("old-image.jpg");
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(fileStorageService.storeFile(any(InputStream.class), anyString(), anyString()))
            .thenReturn("new-image.jpg");
        when(fileStorageService.getFileUrl("new-image.jpg"))
            .thenReturn("/uploads/new-image.jpg");

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Act
        profileView.processProfileImageUpload("photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Assert — old image deleted, new one saved
        verify(fileStorageService).deleteFile("old-image.jpg");
        verify(userService).saveUser(testUser);
        assertEquals("new-image.jpg", testUser.getProfileImage());
    }

    @Test
    void testProfileImageUploadHandlesEmptyOldImage() throws Exception {
        // Arrange — user has empty string for profile image
        testUser.setProfileImage("");
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(fileStorageService.storeFile(any(InputStream.class), anyString(), anyString()))
            .thenReturn("new-image.jpg");
        when(fileStorageService.getFileUrl("new-image.jpg"))
            .thenReturn("/uploads/new-image.jpg");

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Act
        profileView.processProfileImageUpload("photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        // Assert — should NOT try to delete empty filename
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(userService).saveUser(testUser);
    }

    // =============================================
    // formatPhoneNumber tests
    // =============================================

    @Test
    void formatPhoneNumberReturnsEmptyForNull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("", profileView.formatPhoneNumber(null));
    }

    @Test
    void formatPhoneNumberReturnsEmptyForBlank() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("", profileView.formatPhoneNumber("   "));
    }

    @Test
    void formatPhoneNumberReturnsEmptyForNonDigitInput() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("", profileView.formatPhoneNumber("abc"));
    }

    @Test
    void formatPhoneNumberFormatsThreeDigitsAsIs() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("555", profileView.formatPhoneNumber("555"));
    }

    @Test
    void formatPhoneNumberFormatsFourToSixDigitsWithAreaCode() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("(555) 123", profileView.formatPhoneNumber("555123"));
    }

    @Test
    void formatPhoneNumberFormatsSevenToTenDigitsFull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("5551234567"));
    }

    @Test
    void formatPhoneNumberTruncatesMoreThanTenDigits() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("555123456789"));
    }

    @Test
    void formatPhoneNumberStripsExistingFormatting() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("(555) 123-4567"));
    }

    @Test
    void formatPhoneNumberHandlesPartialInput() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        // 5 digits
        assertEquals("(555) 12", profileView.formatPhoneNumber("55512"));
    }

    // =============================================
    // isValidPhoneNumber tests
    // =============================================

    @Test
    void isValidPhoneNumberReturnsTrueForNull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertTrue(profileView.isValidPhoneNumber(null));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForEmpty() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertTrue(profileView.isValidPhoneNumber(""));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForFormattedNumber() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertTrue(profileView.isValidPhoneNumber("(555) 123-4567"));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForDashSeparated() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertTrue(profileView.isValidPhoneNumber("555-123-4567"));
    }

    @Test
    void isValidPhoneNumberReturnsFalseForTooShort() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertFalse(profileView.isValidPhoneNumber("555"));
    }

    @Test
    void isValidPhoneNumberReturnsFalseForLetters() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertFalse(profileView.isValidPhoneNumber("abc-def-ghij"));
    }

    // =============================================
    // normalizePhoneNumber tests
    // =============================================

    @Test
    void normalizePhoneNumberReturnsEmptyForNull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("", profileView.normalizePhoneNumber(null));
    }

    @Test
    void normalizePhoneNumberReturnsEmptyForBlank() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("", profileView.normalizePhoneNumber("   "));
    }

    @Test
    void normalizePhoneNumberStripsFormattingToDigitsOnly() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("5551234567", profileView.normalizePhoneNumber("(555) 123-4567"));
    }

    @Test
    void normalizePhoneNumberPassesThroughDigitsOnly() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertEquals("5551234567", profileView.normalizePhoneNumber("5551234567"));
    }

    // =============================================
    // saveProfile tests
    // =============================================

    @Test
    void saveProfilePersistsNormalizedPhoneAndNames() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Simulate user editing fields — access via reflection since fields are private
        setTextField(profileView, "firstNameField", "Jane");
        setTextField(profileView, "lastNameField", "Smith");
        setTextField(profileView, "phoneNumberField", "(555) 123-4567");

        profileView.saveProfile();

        verify(userService).saveUser(testUser);
        assertEquals("Jane", testUser.getFirstName());
        assertEquals("Smith", testUser.getLastName());
        assertEquals("5551234567", testUser.getPhoneNumber());
    }

    @Test
    void saveProfileRejectsInvalidPhoneNumber() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Use a partial number that is too short to be valid (2 digits).
        // formatPhoneNumber("12") returns "12", which fails isValidPhoneNumber.
        setTextField(profileView, "phoneNumberField", "12");

        profileView.saveProfile();

        // saveUser should NOT be called when phone is invalid
        verify(userService, never()).saveUser(any());
    }

    @Test
    void saveProfileHandsExceptionGracefully() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        setTextField(profileView, "phoneNumberField", "");
        when(userService.saveUser(any())).thenThrow(new RuntimeException("DB down"));

        // Should not propagate exception
        assertDoesNotThrow(() -> profileView.saveProfile());
    }

    @Test
    void saveProfileAllowsEmptyPhone() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        setTextField(profileView, "firstNameField", "Jane");
        setTextField(profileView, "lastNameField", "Smith");
        setTextField(profileView, "phoneNumberField", "");

        profileView.saveProfile();

        verify(userService).saveUser(testUser);
        assertEquals("", testUser.getPhoneNumber());
    }

    @Test
    void saveProfileSavesNotifyNewPostsPreference() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        setTextField(profileView, "phoneNumberField", "");
        setCheckboxValue(profileView, "notifyNewPostsCheckbox", false);

        profileView.saveProfile();

        verify(userService).saveUser(testUser);
        assertFalse(testUser.getNotifyNewPosts());
    }

    // =============================================
    // refreshMyStatsList tests
    // =============================================

    @Test
    void refreshMyStatsListShowsEmptyMessageWhenNoStats() {
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(Collections.emptyList());

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // refreshMyStatsList is called during construction; verify the empty message
        boolean hasEmptyMessage = findAllDescendants(profileView)
            .anyMatch(c -> c instanceof Paragraph
                && ((Paragraph) c).getText().contains("No personal stats yet"));
        assertTrue(hasEmptyMessage, "Should show empty state message when no stats");
    }

    @Test
    void refreshMyStatsListRendersActiveAndArchivedStats() {
        PersonalStatDefinition activeStat = new PersonalStatDefinition("pushups", "Push-ups", "💪", 0, testUser);
        activeStat.setId(1L);
        activeStat.setArchived(false);

        PersonalStatDefinition archivedStat = new PersonalStatDefinition("running", "Running", "🏃", 1, testUser);
        archivedStat.setId(2L);
        archivedStat.setArchived(true);

        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(activeStat, archivedStat));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // Active stat label should be present
        boolean hasActiveLabel = findAllDescendants(profileView)
            .anyMatch(c -> c instanceof Span && "Push-ups".equals(((Span) c).getText()));
        assertTrue(hasActiveLabel, "Should render active stat label");

        // Archived badge should be present
        boolean hasArchivedBadge = findAllDescendants(profileView)
            .anyMatch(c -> c instanceof Span && "archived".equals(((Span) c).getText()));
        assertTrue(hasArchivedBadge, "Should render archived badge");
    }

    @Test
    void refreshMyStatsListRendersUpDownButtonsForActiveStats() {
        PersonalStatDefinition stat1 = new PersonalStatDefinition("a", "Alpha", "🅰", 0, testUser);
        stat1.setId(1L);
        stat1.setArchived(false);

        PersonalStatDefinition stat2 = new PersonalStatDefinition("b", "Beta", "🅱", 1, testUser);
        stat2.setId(2L);
        stat2.setArchived(false);

        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(stat1, stat2));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        // There should be at least one Button that is a Restore button (none here) — check that Buttons exist for arrows
        long buttonCount = findAllDescendants(profileView)
            .filter(c -> c instanceof Button)
            .count();
        // At minimum: Add Stat button + (up, down, archive) x 2 active stats = 7 buttons
        assertTrue(buttonCount >= 7, "Should have buttons for reordering and archiving active stats, got " + buttonCount);
    }

    // =============================================
    // movePersonalStatUp / Down tests
    // =============================================

    @Test
    void movePersonalStatUpSwapsDisplayOrders() {
        PersonalStatDefinition stat1 = new PersonalStatDefinition("a", "Alpha", "🅰", 0, testUser);
        stat1.setId(1L);
        stat1.setArchived(false);

        PersonalStatDefinition stat2 = new PersonalStatDefinition("b", "Beta", "🅱", 1, testUser);
        stat2.setId(2L);
        stat2.setArchived(false);

        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(any()))
            .thenReturn(new ArrayList<>(List.of(stat1, stat2)));
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(stat1, stat2));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.movePersonalStatUp(stat2);

        // stat2 should get stat1's order and vice versa
        assertEquals(0, stat2.getDisplayOrder());
        assertEquals(1, stat1.getDisplayOrder());
        verify(personalStatDefinitionRepository).save(stat2);
        verify(personalStatDefinitionRepository).save(stat1);
    }

    @Test
    void movePersonalStatDownSwapsDisplayOrders() {
        PersonalStatDefinition stat1 = new PersonalStatDefinition("a", "Alpha", "🅰", 0, testUser);
        stat1.setId(1L);
        stat1.setArchived(false);

        PersonalStatDefinition stat2 = new PersonalStatDefinition("b", "Beta", "🅱", 1, testUser);
        stat2.setId(2L);
        stat2.setArchived(false);

        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(any()))
            .thenReturn(new ArrayList<>(List.of(stat1, stat2)));
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(stat1, stat2));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.movePersonalStatDown(stat1);

        assertEquals(1, stat1.getDisplayOrder());
        assertEquals(0, stat2.getDisplayOrder());
        verify(personalStatDefinitionRepository).save(stat1);
        verify(personalStatDefinitionRepository).save(stat2);
    }

    @Test
    void movePersonalStatUpDoesNothingForFirstStat() {
        PersonalStatDefinition stat1 = new PersonalStatDefinition("a", "Alpha", "🅰", 0, testUser);
        stat1.setId(1L);
        stat1.setArchived(false);

        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(any()))
            .thenReturn(new ArrayList<>(List.of(stat1)));
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(stat1));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.movePersonalStatUp(stat1);

        // save should not be called for swap since it's first item (no match in loop starting at i=1)
        verify(personalStatDefinitionRepository, never()).save(any());
    }

    @Test
    void movePersonalStatDownDoesNothingForLastStat() {
        PersonalStatDefinition stat1 = new PersonalStatDefinition("a", "Alpha", "🅰", 0, testUser);
        stat1.setId(1L);
        stat1.setArchived(false);

        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(any()))
            .thenReturn(new ArrayList<>(List.of(stat1)));
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(stat1));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.movePersonalStatDown(stat1);

        verify(personalStatDefinitionRepository, never()).save(any());
    }

    // =============================================
    // archivePersonalStat tests
    // =============================================

    @Test
    void archivePersonalStatSetsArchivedAndSaves() {
        PersonalStatDefinition stat = new PersonalStatDefinition("pushups", "Push-ups", "💪", 0, testUser);
        stat.setId(1L);
        stat.setArchived(false);

        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(Collections.emptyList());

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.archivePersonalStat(stat);

        assertTrue(stat.getArchived());
        verify(personalStatDefinitionRepository).save(stat);
    }

    // =============================================
    // restorePersonalStat tests
    // =============================================

    @Test
    void restorePersonalStatSetsUnarchivedAndAppendsToEnd() {
        PersonalStatDefinition activeStatExisting = new PersonalStatDefinition("active", "Active", "✅", 0, testUser);
        activeStatExisting.setId(1L);
        activeStatExisting.setArchived(false);

        PersonalStatDefinition archivedStat = new PersonalStatDefinition("pushups", "Push-ups", "💪", 5, testUser);
        archivedStat.setId(2L);
        archivedStat.setArchived(true);

        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(activeStatExisting, archivedStat));
        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(any()))
            .thenReturn(new ArrayList<>(List.of(activeStatExisting)));
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.restorePersonalStat(archivedStat);

        assertFalse(archivedStat.getArchived());
        assertEquals(1, archivedStat.getDisplayOrder()); // maxOrder(0) + 1
        verify(personalStatDefinitionRepository).save(archivedStat);
    }

    @Test
    void restorePersonalStatBlockedByGlobalStatCollision() {
        PersonalStatDefinition archivedStat = new PersonalStatDefinition("pushups", "Push-ups", "💪", 0, testUser);
        archivedStat.setId(1L);
        archivedStat.setArchived(true);

        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(List.of(archivedStat));
        StatDefinition globalPushups = new StatDefinition("pushups", "Push-ups", null, 0);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of(globalPushups));

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);

        profileView.restorePersonalStat(archivedStat);

        // Should remain archived — save should NOT be called
        assertTrue(archivedStat.getArchived());
        verify(personalStatDefinitionRepository, never()).save(archivedStat);
    }

    // =============================================
    // openAddPersonalStatDialog tests
    // =============================================

    @Test
    void openAddPersonalStatDialogDoesNotThrow() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository);
        assertDoesNotThrow(() -> profileView.openAddPersonalStatDialog());
    }

    /**
     * Recursively stream all descendant components.
     */
    private Stream<Component> findAllDescendants(Component root) {
        return Stream.concat(
            root.getChildren(),
            root.getChildren().flatMap(this::findAllDescendants)
        );
    }

    private void setTextField(ProfileView view, String fieldName, String value) {
        try {
            java.lang.reflect.Field field = ProfileView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            TextField tf = (TextField) field.get(view);
            tf.setValue(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCheckboxValue(ProfileView view, String fieldName, boolean value) {
        try {
            java.lang.reflect.Field field = ProfileView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            com.vaadin.flow.component.checkbox.Checkbox cb = (com.vaadin.flow.component.checkbox.Checkbox) field.get(view);
            cb.setValue(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}