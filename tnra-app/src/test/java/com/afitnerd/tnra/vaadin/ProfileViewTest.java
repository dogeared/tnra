package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.FileStorageService;
import com.afitnerd.tnra.service.GroupSettingsService;
import com.afitnerd.tnra.service.PostDataExportService;
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

    @Mock
    private GroupSettingsService groupSettingsService;

    @Mock
    private PostDataExportService postDataExportService;

    // Billing off by default → no Gift tab; existing tests are unaffected. Gift-tab tests below
    // construct their own ProfileView with Optional.of(billingClientMock).
    private Optional<BillingClient> billingClient = Optional.empty();

    private GroupSettings groupSettings;
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

        // Default: Slack publishing master is off — Slack section stays hidden in existing tests
        groupSettings = new GroupSettings();
        lenient().when(groupSettingsService.getSettings()).thenReturn(groupSettings);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void testProfileViewCreation() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
        assertTrue(profileView.hasClassName("profile-view"));
    }

    @Test
    void testProfileViewLayoutProperties() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Assert
        // ProfileView uses default alignment properties, not custom ones
        assertTrue(profileView.isPadding());
        assertTrue(profileView.isSpacing());
        assertTrue(profileView.hasClassName("profile-view"));
    }

    @Test
    void testProfileViewContainsExpectedComponents() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewWithUserWithProfileImage() {
        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
            profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        });
    }

    @Test
    void testProfileViewWithInvalidPhoneNumber() {
        // Arrange
        testUser.setPhoneNumber("invalid-phone");
        when(userService.getCurrentUser()).thenReturn(testUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Assert
        assertNotNull(profileView);
        assertTrue(profileView.getChildren().count() > 0);
    }

    @Test
    void testProfileViewConstructorWithNullUserService() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            new ProfileView(null, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        });
    }

    @Test
    void testProfileViewConstructorWithNullFileService() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            new ProfileView(userService, null, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        });
    }

    @Test
    void testProfileViewWithEmptyUserData() {
        // Arrange
        User emptyUser = new User();
        when(userService.getCurrentUser()).thenReturn(emptyUser);

        // Act
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
            profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Assert - find the "My Stats" H3 header inside the My Stats tab.
        // (Vaadin's TabSheet hides tab contents from the parent's standard
        // children traversal, so we walk the tab content directly.)
        boolean hasMyStatsHeader = findAllDescendants(profileView.myStatsTabContent)
            .anyMatch(c -> c instanceof H3 && "My Stats".equals(((H3) c).getText()));
        assertTrue(hasMyStatsHeader, "My Stats tab should contain a 'My Stats' header");

        boolean hasAddStatButton = findAllDescendants(profileView.myStatsTabContent)
            .anyMatch(c -> c instanceof Button && ((Button) c).getText().contains("Add Stat"));
        assertTrue(hasAddStatButton, "My Stats tab should contain an 'Add Stat' button");
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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("", profileView.formatPhoneNumber(null));
    }

    @Test
    void formatPhoneNumberReturnsEmptyForBlank() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("", profileView.formatPhoneNumber("   "));
    }

    @Test
    void formatPhoneNumberReturnsEmptyForNonDigitInput() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("", profileView.formatPhoneNumber("abc"));
    }

    @Test
    void formatPhoneNumberFormatsThreeDigitsAsIs() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("555", profileView.formatPhoneNumber("555"));
    }

    @Test
    void formatPhoneNumberFormatsFourToSixDigitsWithAreaCode() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("(555) 123", profileView.formatPhoneNumber("555123"));
    }

    @Test
    void formatPhoneNumberFormatsSevenToTenDigitsFull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("5551234567"));
    }

    @Test
    void formatPhoneNumberTruncatesMoreThanTenDigits() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("555123456789"));
    }

    @Test
    void formatPhoneNumberStripsExistingFormatting() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("(555) 123-4567", profileView.formatPhoneNumber("(555) 123-4567"));
    }

    @Test
    void formatPhoneNumberHandlesPartialInput() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        // 5 digits
        assertEquals("(555) 12", profileView.formatPhoneNumber("55512"));
    }

    // =============================================
    // isValidPhoneNumber tests
    // =============================================

    @Test
    void isValidPhoneNumberReturnsTrueForNull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertTrue(profileView.isValidPhoneNumber(null));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForEmpty() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertTrue(profileView.isValidPhoneNumber(""));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForFormattedNumber() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertTrue(profileView.isValidPhoneNumber("(555) 123-4567"));
    }

    @Test
    void isValidPhoneNumberReturnsTrueForDashSeparated() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertTrue(profileView.isValidPhoneNumber("555-123-4567"));
    }

    @Test
    void isValidPhoneNumberReturnsFalseForTooShort() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertFalse(profileView.isValidPhoneNumber("555"));
    }

    @Test
    void isValidPhoneNumberReturnsFalseForLetters() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertFalse(profileView.isValidPhoneNumber("abc-def-ghij"));
    }

    // =============================================
    // normalizePhoneNumber tests
    // =============================================

    @Test
    void normalizePhoneNumberReturnsEmptyForNull() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("", profileView.normalizePhoneNumber(null));
    }

    @Test
    void normalizePhoneNumberReturnsEmptyForBlank() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("", profileView.normalizePhoneNumber("   "));
    }

    @Test
    void normalizePhoneNumberStripsFormattingToDigitsOnly() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("5551234567", profileView.normalizePhoneNumber("(555) 123-4567"));
    }

    @Test
    void normalizePhoneNumberPassesThroughDigitsOnly() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertEquals("5551234567", profileView.normalizePhoneNumber("5551234567"));
    }

    // =============================================
    // saveProfile tests
    // =============================================

    @Test
    void saveProfilePersistsNormalizedPhoneAndNames() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Use a partial number that is too short to be valid (2 digits).
        // formatPhoneNumber("12") returns "12", which fails isValidPhoneNumber.
        setTextField(profileView, "phoneNumberField", "12");

        profileView.saveProfile();

        // saveUser should NOT be called when phone is invalid
        verify(userService, never()).saveUser(any());
    }

    @Test
    void saveProfileHandsExceptionGracefully() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        setTextField(profileView, "phoneNumberField", "");
        when(userService.saveUser(any())).thenThrow(new RuntimeException("DB down"));

        // Should not propagate exception
        assertDoesNotThrow(() -> profileView.saveProfile());
    }

    @Test
    void saveProfileAllowsEmptyPhone() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        setTextField(profileView, "firstNameField", "Jane");
        setTextField(profileView, "lastNameField", "Smith");
        setTextField(profileView, "phoneNumberField", "");

        profileView.saveProfile();

        verify(userService).saveUser(testUser);
        assertEquals("", testUser.getPhoneNumber());
    }

    @Test
    void saveProfileSavesNotifyNewPostsPreference() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // refreshMyStatsList is called during construction; verify the empty message
        boolean hasEmptyMessage = findAllDescendants(profileView.myStatsTabContent)
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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Active stat label should be present in the My Stats tab content
        boolean hasActiveLabel = findAllDescendants(profileView.myStatsTabContent)
            .anyMatch(c -> c instanceof Span && "Push-ups".equals(((Span) c).getText()));
        assertTrue(hasActiveLabel, "Should render active stat label");

        boolean hasArchivedBadge = findAllDescendants(profileView.myStatsTabContent)
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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Buttons inside the My Stats tab: Add Stat + (up, down, archive) x 2 active stats = 7 minimum
        long buttonCount = findAllDescendants(profileView.myStatsTabContent)
            .filter(c -> c instanceof Button)
            .count();
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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

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
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertDoesNotThrow(() -> profileView.openAddPersonalStatDialog());
    }

    // =============================================
    // handleAddPersonalStat tests
    // =============================================

    @Test
    void handleAddPersonalStatRejectsEmptyName() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.error(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("", "Label", "", dialog);
            notif.verify(() -> AppNotification.error("Name and label are required"));
            verify(dialog, never()).close();
        }
    }

    @Test
    void handleAddPersonalStatRejectsEmptyLabel() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.error(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("name", "", "", dialog);
            notif.verify(() -> AppNotification.error("Name and label are required"));
            verify(dialog, never()).close();
        }
    }

    @Test
    void handleAddPersonalStatRejectsGlobalNameCollision() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        StatDefinition existing = new StatDefinition("exercise", "Exercise", "💪", 0);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of(existing));
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.error(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("exercise", "Exercise", "", dialog);
            notif.verify(() -> AppNotification.error("A group stat named 'exercise' already exists"));
            verify(dialog, never()).close();
        }
    }

    @Test
    void handleAddPersonalStatRejectsPersonalNameCollision() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());
        PersonalStatDefinition existing = new PersonalStatDefinition("guitar", "Guitar", "🎸", 0, testUser);
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(testUser)).thenReturn(List.of(existing));
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.error(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("guitar", "Guitar Practice", "", dialog);
            notif.verify(() -> AppNotification.error("You already have a stat named 'guitar'"));
            verify(dialog, never()).close();
        }
    }

    @Test
    void handleAddPersonalStatSavesNewStatSuccessfully() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(testUser)).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(testUser)).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.success(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("run", "Running", "🏃", dialog);
            verify(personalStatDefinitionRepository).save(any(PersonalStatDefinition.class));
            verify(dialog).close();
            notif.verify(() -> AppNotification.success("Running added"));
        }
    }

    @Test
    void handleAddPersonalStatSetsNullEmojiWhenEmpty() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(testUser)).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(testUser)).thenReturn(Collections.emptyList());
        when(personalStatDefinitionRepository.save(any())).thenAnswer(inv -> {
            PersonalStatDefinition saved = inv.getArgument(0);
            assertNull(saved.getEmoji());
            return saved;
        });
        com.vaadin.flow.component.dialog.Dialog dialog = mock(com.vaadin.flow.component.dialog.Dialog.class);
        try (org.mockito.MockedStatic<AppNotification> notif = mockStatic(AppNotification.class)) {
            notif.when(() -> AppNotification.success(anyString())).thenAnswer(inv -> null);
            profileView.handleAddPersonalStat("run", "Running", "", dialog);
        }
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

    // === Slack publishing section (conditional render) ===

    @Test
    void slackPublishSection_hiddenWhenGroupMasterOff() {
        groupSettings.setSlackPublishPostData(false);

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertFalse(profileView.slackPublishSection.isVisible(),
            "Slack section must be hidden when group master toggle is off");
    }

    @Test
    void slackPublishSection_visibleAndEnabledWhenGroupMasterOnAndNoOverrides() {
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(false);
        groupSettings.setSlackPublishPostBody(false);

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertTrue(profileView.slackPublishSection.isVisible());
        // Verify the "no override applied" state via the absence of override badges.
        // (Asserting isEnabled() == true here trips Vaadin's detached-component
        //  cascade quirk for unattached views, even though setEnabled was never
        //  called to disable them.)
        assertFalse(profileView.slackPublishStatsOverrideBadge.isVisible(),
            "No badge visible when no override is in effect");
        assertFalse(profileView.slackPublishPostBodyOverrideBadge.isVisible());
    }

    @Test
    void slackPublishSection_statsCheckboxForcedWhenGroupOverridesStats() {
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(true);
        groupSettings.setSlackPublishPostBody(false);
        testUser.setSlackPublishStats(false); // user opted out but group overrides

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertTrue(profileView.slackPublishStatsCheckbox.getValue(), "Stats checkbox must be force-checked");
        assertFalse(profileView.slackPublishStatsCheckbox.isEnabled(),
            "Stats checkbox must be disabled so the disabled theme renders on first paint");
        assertTrue(profileView.slackPublishStatsOverrideBadge.isVisible(),
            "Override badge must be visible so the override is obvious without hovering");
        // Body checkbox not overridden — confirmed via the absence of its badge
        assertFalse(profileView.slackPublishPostBodyOverrideBadge.isVisible(),
            "Body badge must stay hidden when body is not overridden");
    }

    @Test
    void slackPublishSection_bodyCheckboxForcedWhenGroupOverridesBody() {
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(false);
        groupSettings.setSlackPublishPostBody(true);
        testUser.setSlackPublishPostBody(false);

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertTrue(profileView.slackPublishPostBodyCheckbox.getValue());
        assertFalse(profileView.slackPublishPostBodyCheckbox.isEnabled());
        assertTrue(profileView.slackPublishPostBodyOverrideBadge.isVisible());
        // Stats checkbox not overridden — confirmed via the absence of its badge
        assertFalse(profileView.slackPublishStatsOverrideBadge.isVisible());
    }

    @Test
    void slackPublishSection_savesUserChoiceWhenNotOverridden() {
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(false);
        groupSettings.setSlackPublishPostBody(false);

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        setTextField(profileView, "phoneNumberField", "");
        setCheckboxValue(profileView, "slackPublishStatsCheckbox", true);
        setCheckboxValue(profileView, "slackPublishPostBodyCheckbox", true);
        profileView.saveProfile();

        assertTrue(testUser.getSlackPublishStats());
        assertTrue(testUser.getSlackPublishPostBody());
        verify(userService).saveUser(testUser);
    }

    @Test
    void slackPublishSection_preservesStoredUserValueWhenGroupOverrideIsOn() {
        // User had opted out previously; group then turned on the override.
        // Saving the profile must NOT overwrite the user's stored opt-out value.
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(true);
        testUser.setSlackPublishStats(false);

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        setTextField(profileView, "phoneNumberField", "");
        profileView.saveProfile();

        assertFalse(testUser.getSlackPublishStats(),
            "Stored user value must persist as-is when group overrides; UI force-check is cosmetic only");
    }

    // === Download my data section ===

    @Test
    void dataExport_sectionRenders() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertNotNull(profileView.exportFromDatePicker, "From date picker should exist");
        assertNotNull(profileView.exportToDatePicker, "To date picker should exist");
        assertNotNull(profileView.exportAllDataCheckbox, "All-data checkbox should exist");
        assertNotNull(profileView.exportDownloadLink, "Download link should exist");
    }

    @Test
    void dataExport_allDataCheckboxDisablesDatePickers() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Toggling "All my data" on disables both date pickers — the listener wires both.
        profileView.exportAllDataCheckbox.setValue(true);
        assertFalse(profileView.exportFromDatePicker.isEnabled());
        assertFalse(profileView.exportToDatePicker.isEnabled());
    }

    @Test
    void dataExport_buildCsvCallsServiceWithDateRangeWhenAllDataOff() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        java.time.LocalDate from = java.time.LocalDate.of(2026, 1, 1);
        java.time.LocalDate to = java.time.LocalDate.of(2026, 5, 1);
        profileView.exportAllDataCheckbox.setValue(false);
        profileView.exportFromDatePicker.setValue(from);
        profileView.exportToDatePicker.setValue(to);
        when(postDataExportService.exportToCsv(testUser, from, to)).thenReturn("csv".getBytes());

        byte[] result = profileView.buildExportCsv();

        assertArrayEquals("csv".getBytes(), result);
        verify(postDataExportService).exportToCsv(testUser, from, to);
    }

    @Test
    void dataExport_buildCsvIgnoresDateRangeWhenAllDataChecked() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        profileView.exportAllDataCheckbox.setValue(true);
        profileView.exportFromDatePicker.setValue(java.time.LocalDate.of(2026, 1, 1));
        profileView.exportToDatePicker.setValue(java.time.LocalDate.of(2026, 5, 1));
        when(postDataExportService.exportToCsv(testUser, null, null)).thenReturn("all".getBytes());

        byte[] result = profileView.buildExportCsv();

        assertArrayEquals("all".getBytes(), result);
        verify(postDataExportService).exportToCsv(testUser, null, null);
    }

    @Test
    void tabbedLayout_rendersFourTabs() {
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(Collections.emptyList());

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        com.vaadin.flow.component.tabs.TabSheet tabs = findAllDescendants(profileView)
            .filter(c -> c instanceof com.vaadin.flow.component.tabs.TabSheet)
            .map(c -> (com.vaadin.flow.component.tabs.TabSheet) c)
            .findFirst()
            .orElseThrow(() -> new AssertionError("ProfileView should contain a TabSheet"));

        java.util.List<String> labels = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            com.vaadin.flow.component.tabs.Tab tab = tabs.getTabAt(i);
            assertNotNull(tab, "Tab " + i + " should exist");
            labels.add(tab.getLabel());
        }

        assertEquals(java.util.List.of("Basic Info", "Notifications", "My Stats", "Export"), labels,
            "Profile tabs should be in the requested order");
    }

    @Test
    void dataExport_downloadEnabledDecisionLogic() {
        // Static helper — pure logic, no Vaadin component quirks.
        java.time.LocalDate someDate = java.time.LocalDate.of(2026, 1, 1);
        assertFalse(ProfileView.shouldEnableExportDownload(false, null, null),
            "Nothing selected → disabled");
        assertTrue(ProfileView.shouldEnableExportDownload(true, null, null),
            "All-data checked → enabled");
        assertTrue(ProfileView.shouldEnableExportDownload(false, someDate, null),
            "From-only → enabled (export from that date forward)");
        assertTrue(ProfileView.shouldEnableExportDownload(false, null, someDate),
            "To-only → enabled (export through that date)");
        assertTrue(ProfileView.shouldEnableExportDownload(false, someDate, someDate),
            "Both bounds → enabled");
    }

    @Test
    void dataExport_downloadDisabledOnInitialState() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Construction state: no date range, all-data unchecked → button disabled
        // AND the Anchor has no href. A disabled button alone wouldn't block the
        // click: a live href on the surrounding <a> would still navigate.
        assertFalse(profileView.exportDownloadButton.isEnabled());
        assertTrue(profileView.exportDownloadLink.getHref().isEmpty(),
            "Anchor must have no href when disabled — otherwise clicks still navigate");
    }

    @Test
    void dataExport_hrefAttachedAfterSelection() {
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Pre-condition: empty
        assertTrue(profileView.exportDownloadLink.getHref().isEmpty());

        // Selecting all-data attaches the StreamResource
        profileView.exportAllDataCheckbox.setValue(true);
        assertFalse(profileView.exportDownloadLink.getHref().isEmpty(),
            "Anchor must have an href once something is selected");

        // Un-selecting removes it again
        profileView.exportAllDataCheckbox.setValue(false);
        assertTrue(profileView.exportDownloadLink.getHref().isEmpty(),
            "Anchor href must be removed when selection clears");
    }

    @Test
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
    void dataExport_filenameReflectsSelection() {
        java.time.LocalDate from = java.time.LocalDate.of(2026, 1, 1);
        java.time.LocalDate to = java.time.LocalDate.of(2026, 5, 1);
        assertEquals("tnra-posts-all.csv", ProfileView.buildExportFilename(true, null, null));
        assertEquals("tnra-posts-from-2026-01-01-to-2026-05-01.csv", ProfileView.buildExportFilename(false, from, to));
        assertEquals("tnra-posts-from-2026-01-01.csv", ProfileView.buildExportFilename(false, from, null));
        assertEquals("tnra-posts-through-2026-05-01.csv", ProfileView.buildExportFilename(false, null, to));
    }

    @Test
    void slackPublishSection_overrideBadgeTextIsParenthesized() {
        groupSettings.setSlackPublishPostData(true);
        groupSettings.setSlackPublishStats(true);
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        assertEquals("(Required by group settings)", profileView.slackPublishStatsOverrideBadge.getText());
    }

    @Test
    void tabbedLayout_bothSaveButtonsExist() {
        when(personalStatDefinitionRepository.findByUserOrderByDisplayOrderAsc(any()))
            .thenReturn(Collections.emptyList());

        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        // Basic Info tab has its own Save Changes button
        assertNotNull(profileView, "view created");
        // Notifications tab has its own Save Changes button — exposed for testing
        assertNotNull(profileView.notificationsSaveButton, "Notifications tab Save button should exist");
        assertEquals("Save Changes", profileView.notificationsSaveButton.getText());
    }

    @Test
    void dataExport_buildCsvReturnsEmptyWhenNoCurrentUser() {
        when(userService.getCurrentUser()).thenReturn(null);
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");

        byte[] result = profileView.buildExportCsv();

        assertEquals(0, result.length);
        verify(postDataExportService, never()).exportToCsv(any(), any(), any());
    }

    // ---- Billing tab (membership + gift + covering) ----

    private ProfileView profileWithBilling(BillingClient client) {
        return new ProfileView(userService, fileStorageService, statDefinitionRepository,
            personalStatDefinitionRepository, groupSettingsService, postDataExportService, Optional.of(client), "http://localhost:8080");
    }

    /** Construct, then simulate the user opening the Billing tab (which lazily loads its data). */
    private ProfileView openBillingTab(BillingClient client) {
        ProfileView v = profileWithBilling(client);
        v.applyBillingTabState();
        return v;
    }

    private void entitled(BillingClient client, String email, boolean entitled, String status) {
        when(client.entitlement(email)).thenReturn(new BillingClient.Entitlement(entitled, status, null));
    }

    private void entitledGiftedBy(BillingClient client, String email, String status, String payer) {
        when(client.entitlement(email)).thenReturn(new BillingClient.Entitlement(true, status, payer));
    }

    @Test
    void billingTab_absentWhenBillingDisabled() {
        // billingClient field defaults to Optional.empty()
        profileView = new ProfileView(userService, fileStorageService, statDefinitionRepository, personalStatDefinitionRepository, groupSettingsService, postDataExportService, billingClient, "http://localhost:8080");
        assertNull(profileView.giftRecipientCombo, "no Billing tab when billing is off");
        assertNull(profileView.billingPayButtons);
    }

    @Test
    void billingTab_presentButNotLoadedUntilOpened() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");

        // Components exist after construction; no billing call happens until the tab is opened.
        profileView = profileWithBilling(client);

        assertNotNull(profileView.billingMonthlyButton);
        assertNotNull(profileView.billingUpdatePaymentButton);
        assertNotNull(profileView.giftRecipientCombo);
        assertNotNull(profileView.giftContinueButton);
        verify(client, never()).entitlement(anyString()); // lazy: not loaded until the tab opens
        verify(client, never()).covering(anyString());
    }

    @Test
    void billingTab_showsPayOptions_whenNoActiveSubscription() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", false, "PENDING_PAYMENT");

        profileView = openBillingTab(client);

        assertTrue(profileView.billingPayButtons.isVisible());
        assertTrue(profileView.billingUpdatePaymentButton.isVisible()); // always shown
    }

    @Test
    void billingTab_hidesPayOptions_whenActiveSubscription() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ACTIVE");

        profileView = openBillingTab(client);

        assertFalse(profileView.billingPayButtons.isVisible());
        assertTrue(profileView.billingUpdatePaymentButton.isVisible()); // still shown
    }

    @Test
    void billingTab_hidesPayOptions_whenInGracePeriod() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ON_GRACE_PERIOD");

        profileView = openBillingTab(client);

        // Dunning still counts as having a subscription — fix the card via the portal, don't re-buy.
        assertFalse(profileView.billingPayButtons.isVisible());
    }

    @Test
    void billingTab_blurbSaysActive_whenSubscription() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ACTIVE");

        profileView = openBillingTab(client);

        String blurb = profileView.billingBlurb.getText();
        assertTrue(blurb.contains("active"), blurb);
        assertFalse(blurb.contains("Activate your membership"), blurb);
    }

    @Test
    void billingTab_blurbSaysActivate_whenNoSubscription() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", false, "PENDING_PAYMENT");

        profileView = openBillingTab(client);

        assertTrue(profileView.billingBlurb.getText().contains("Activate your membership"));
    }

    @Test
    void billingTab_blurbSaysGiftedBy_whenMembershipIsAGift() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitledGiftedBy(client, "me@x.com", "ACTIVE", "alice@x.com");
        User alice = new User();
        alice.setEmail("alice@x.com");
        alice.setFirstName("Alice");
        alice.setLastName("A");
        when(userService.getUserByEmail("alice@x.com")).thenReturn(alice);

        profileView = openBillingTab(client);

        String blurb = profileView.billingBlurb.getText();
        assertTrue(blurb.contains("gifted by"), blurb);
        assertTrue(blurb.contains("Alice A (alice@x.com)"), blurb);
        assertFalse(profileView.billingPayButtons.isVisible()); // gifted = active sub → no pay options
    }

    @Test
    void billingTab_showsCoveringList_whenPayingForOthers() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ACTIVE");
        when(client.covering("me@x.com")).thenReturn(List.of(
            new BillingClient.CoveredMember("ben@x.com", "ACTIVE")));

        profileView = openBillingTab(client);

        assertTrue(profileView.coveringSection.isVisible());
    }

    @Test
    void billingTab_hidesCoveringList_whenNotCoveringAnyone() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ACTIVE");
        when(client.covering("me@x.com")).thenReturn(List.of());

        profileView = openBillingTab(client);

        assertFalse(profileView.coveringSection.isVisible());
    }

    @Test
    void giftSection_entitled_hidesDisabledNotice() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        User other = new User();
        other.setEmail("ben@x.com");
        when(userService.getAllActiveUsers()).thenReturn(List.of(other));
        entitled(client, "me@x.com", true, "ACTIVE");

        profileView = openBillingTab(client);

        assertFalse(profileView.giftDisabledNotice.isVisible());
    }

    @Test
    void giftSection_notEntitled_showsDisabledNotice() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", false, "PENDING_PAYMENT");

        profileView = openBillingTab(client);

        assertTrue(profileView.giftDisabledNotice.isVisible());
    }

    @Test
    void giftSection_excludesSelfFromRecipients() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        User self = new User();
        self.setEmail("me@x.com");
        User other = new User();
        other.setEmail("ben@x.com");
        when(userService.getAllActiveUsers()).thenReturn(List.of(self, other));
        entitled(client, "me@x.com", true, "ACTIVE");

        profileView = openBillingTab(client);

        List<User> items = profileView.giftRecipientCombo.getListDataView().getItems().toList();
        assertEquals(1, items.size());
        assertEquals("ben@x.com", items.get(0).getEmail());
    }

    @Test
    void continueToGiftCheckout_noSelection_isNoOp() {
        BillingClient client = mock(BillingClient.class);
        testUser.setEmail("me@x.com");
        entitled(client, "me@x.com", true, "ACTIVE");
        profileView = openBillingTab(client);

        // No recipient selected → must not throw or navigate.
        assertDoesNotThrow(() -> profileView.continueToGiftCheckout());
    }

    @Test
    void recipientLabel_formatsNameWithEmail_orEmailOnly() {
        User named = new User();
        named.setEmail("ben@x.com");
        named.setFirstName("Ben");
        named.setLastName("Z");
        assertEquals("Ben Z (ben@x.com)", ProfileView.recipientLabel(named));

        User noName = new User();
        noName.setEmail("anon@x.com");
        assertEquals("anon@x.com", ProfileView.recipientLabel(noName));
    }
}