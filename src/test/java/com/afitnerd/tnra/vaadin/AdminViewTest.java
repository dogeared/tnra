package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.GroupSettingsService;
import com.afitnerd.tnra.service.UserService;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.afitnerd.tnra.vaadin.presenter.VaadinAdminPresenter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminViewTest {

    @Mock
    private VaadinAdminPresenter vaadinAdminPresenter;

    @Mock
    private CallChainPresenter callChainPresenter;

    @Mock
    private StatDefinitionRepository statDefinitionRepository;

    @Mock
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;

    @Mock
    private UserService userService;

    @Mock
    private GroupSettingsService groupSettingsService;

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        org.mockito.Mockito.lenient().when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        when(vaadinAdminPresenter.getGitTag()).thenReturn("v1.0.0");
        when(vaadinAdminPresenter.getGitCommitId()).thenReturn("abc123");
        when(vaadinAdminPresenter.getGitBranch()).thenReturn("main");
        when(vaadinAdminPresenter.getSpringBootVersion()).thenReturn("3.x");
        when(vaadinAdminPresenter.getVaadinVersion()).thenReturn("24.x");
        when(vaadinAdminPresenter.getJavaVersion()).thenReturn("21");
        when(vaadinAdminPresenter.getBuildTime()).thenReturn("2026-03-13T17:00Z");
        lenient().when(userService.getAllActiveUsers()).thenReturn(List.of());
        lenient().when(userService.getAllUsers()).thenReturn(List.of());
        lenient().when(userService.getCurrentUser()).thenReturn(null);
        lenient().when(groupSettingsService.getSettings()).thenReturn(new GroupSettings());
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void buildsHeaderTabsAndCurrentSetGrid() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        assertTrue(view.hasClassName("admin-view"));
        assertTrue(anyComponent(view, H2.class, h -> "Admin Dashboard".equals(h.getText())));
        assertTrue(anyComponent(view, H3.class, h -> "Go To Guy Management".equals(h.getText())));
        assertTrue(anyComponent(view, TabSheet.class, tabs -> true));
        verify(callChainPresenter).getCurrentGoToGuySet();
    }

    @Test
    void createNewSetButtonSuccessPathTogglesUiAndEnablesAddPair() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());
        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        Button addPairButton = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));

        assertTrue(createButton.isEnabled());
        assertFalse(addPairButton.isEnabled());

        assertDoesNotThrow(createButton::click);

        assertFalse(createButton.isEnabled());
        assertTrue(addPairButton.isEnabled());
        verify(callChainPresenter).createNewGoToGuySet(anyList());
    }

    @Test
    void createNewSetButtonFailurePathDoesNotThrow() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenThrow(new RuntimeException("boom"));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));

        assertDoesNotThrow(createButton::click);
        assertTrue(createButton.isEnabled());
    }

    @Test
    void getUserDisplayNameHandlesAllFallbacks() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        Method method = AdminView.class.getDeclaredMethod("getUserDisplayName", User.class);
        method.setAccessible(true);

        User firstLast = new User();
        firstLast.setFirstName("Jane");
        firstLast.setLastName("Smith");
        assertEquals("Jane Smith", method.invoke(view, firstLast));

        User firstOnly = new User();
        firstOnly.setFirstName("Jane");
        assertEquals("Jane", method.invoke(view, firstOnly));

        User emailOnly = new User();
        emailOnly.setEmail("demo@example.com");
        assertEquals("demo", method.invoke(view, emailOnly));

        User idOnly = new User();
        idOnly.setId(42L);
        assertEquals("User #42", method.invoke(view, idOnly));

        assertEquals("Unknown", method.invoke(view, new Object[]{null}));
    }

    @Test
    void findActiveIndexUsesIdNotObjectIdentity() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        // Create stat definitions with IDs (simulating DB-managed objects)
        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰️", 0);
        a.setId(10L);
        StatDefinition b = new StatDefinition("beta", "Beta", "🅱️", 1);
        b.setId(20L);
        StatDefinition c = new StatDefinition("gamma", "Gamma", "🇬", 2);
        c.setId(30L);

        // These are "fresh query result" objects — different Java instances, same IDs
        StatDefinition aFresh = new StatDefinition("alpha", "Alpha", "🅰️", 0);
        aFresh.setId(10L);
        StatDefinition bFresh = new StatDefinition("beta", "Beta", "🅱️", 1);
        bFresh.setId(20L);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        Method method = AdminView.class.getDeclaredMethod("findActiveIndex", List.class, StatDefinition.class);
        method.setAccessible(true);

        List<StatDefinition> activeStats = List.of(a, b, c);

        // Key test: lookup by a DIFFERENT object with the same ID should find the right index
        assertEquals(0, method.invoke(view, activeStats, aFresh));
        assertEquals(1, method.invoke(view, activeStats, bFresh));
        assertEquals(2, method.invoke(view, activeStats, c));

        // Non-existent ID returns -1
        StatDefinition notFound = new StatDefinition("nope", "Nope", "", 0);
        notFound.setId(999L);
        assertEquals(-1, method.invoke(view, activeStats, notFound));
    }

    // =============================================
    // moveStatUp / moveStatDown tests
    // =============================================

    @Test
    void moveStatUpSwapsDisplayOrdersOfAdjacentStats() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);
        StatDefinition b = new StatDefinition("beta", "Beta", "🅱", 1);
        b.setId(2L);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a, b)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.moveStatUp(b, statsList);

        assertEquals(0, b.getDisplayOrder());
        assertEquals(1, a.getDisplayOrder());
        verify(statDefinitionRepository).save(b);
        verify(statDefinitionRepository).save(a);
    }

    @Test
    void moveStatUpDoesNothingWhenAlreadyFirst() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.moveStatUp(a, statsList);

        // index=0, so nothing should be saved
        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    @Test
    void moveStatDownSwapsDisplayOrdersOfAdjacentStats() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);
        StatDefinition b = new StatDefinition("beta", "Beta", "🅱", 1);
        b.setId(2L);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a, b)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.moveStatDown(a, statsList);

        assertEquals(1, a.getDisplayOrder());
        assertEquals(0, b.getDisplayOrder());
        verify(statDefinitionRepository).save(a);
        verify(statDefinitionRepository).save(b);
    }

    @Test
    void moveStatDownDoesNothingWhenAlreadyLast() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.moveStatDown(a, statsList);

        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    // =============================================
    // archiveStat tests
    // =============================================

    @Test
    void archiveStatSetsArchivedAndSaves() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);
        a.setArchived(false);
        StatDefinition b = new StatDefinition("beta", "Beta", "🅱", 1);
        b.setId(2L);
        b.setArchived(false);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a, b)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.archiveStat(a, statsList);

        assertTrue(a.getArchived());
        verify(statDefinitionRepository).save(a);
    }

    @Test
    void archiveStatBlockedWhenOnlyOneActiveStatRemains() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition a = new StatDefinition("alpha", "Alpha", "🅰", 0);
        a.setId(1L);
        a.setArchived(false);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(a)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.archiveStat(a, statsList);

        // Should NOT be archived — the last active stat cannot be archived
        assertFalse(a.getArchived());
        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    // =============================================
    // restoreStat tests
    // =============================================

    @Test
    void restoreStatUnarchivedAndAppendsToEnd() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition active = new StatDefinition("beta", "Beta", "🅱", 0);
        active.setId(2L);
        active.setArchived(false);

        StatDefinition archived = new StatDefinition("alpha", "Alpha", "🅰", 5);
        archived.setId(1L);
        archived.setArchived(true);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>(List.of(active)));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.restoreStat(archived, statsList);

        assertFalse(archived.getArchived());
        assertEquals(1, archived.getDisplayOrder()); // max(0) + 1
        verify(statDefinitionRepository).save(archived);
    }

    @Test
    void restoreStatWhenNoActiveStatsExist() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition archived = new StatDefinition("alpha", "Alpha", "🅰", 5);
        archived.setId(1L);
        archived.setArchived(true);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(new ArrayList<>());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.restoreStat(archived, statsList);

        assertFalse(archived.getArchived());
        assertEquals(0, archived.getDisplayOrder()); // max(-1) + 1 = 0
        verify(statDefinitionRepository).save(archived);
    }

    // =============================================
    // refreshStatsList tests
    // =============================================

    @Test
    void refreshStatsListShowsEmptyStateWhenNoStats() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.refreshStatsList(statsList);

        // Should have an empty state paragraph
        boolean hasEmptyState = statsList.getChildren()
            .anyMatch(c -> c instanceof com.vaadin.flow.component.html.Paragraph);
        assertTrue(hasEmptyState, "Should show empty state when no stats configured");
    }

    @Test
    void refreshStatsListRendersActiveAndArchivedStats() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        StatDefinition active = new StatDefinition("alpha", "Alpha", "🅰", 0);
        active.setId(1L);
        active.setArchived(false);

        StatDefinition archived = new StatDefinition("beta", "Beta", "🅱", 1);
        archived.setId(2L);
        archived.setArchived(true);

        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc())
            .thenReturn(List.of(active, archived));
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(List.of(active));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        view.refreshStatsList(statsList);

        assertEquals(2, statsList.getComponentCount(), "Should have one row per stat");
    }

    // =============================================
    // openAddStatDialog tests
    // =============================================

    @Test
    void openAddStatDialogDoesNotThrow() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout statsList = new VerticalLayout();
        assertDoesNotThrow(() -> view.openAddStatDialog(statsList));
    }

    // =============================================
    // openInviteMemberDialog tests
    // =============================================

    @Test
    void openInviteMemberDialogDoesNotThrow() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        com.vaadin.flow.component.grid.Grid<User> membersGrid = new com.vaadin.flow.component.grid.Grid<>();
        assertDoesNotThrow(() -> view.openInviteMemberDialog(membersGrid));
    }

    // =============================================
    // showValidationError tests
    // =============================================

    @Test
    void showValidationErrorWithNullCallerOrCallee() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        // Should not throw
        assertDoesNotThrow(() -> view.showValidationError(null, null, List.of()));
    }

    @Test
    void showValidationErrorWhenCallerEqualCallee() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        user.setId(1L);
        user.setFirstName("Bob");

        assertDoesNotThrow(() -> view.showValidationError(user, user, List.of()));
    }

    @Test
    void showValidationErrorWhenCalleeAlreadyAssigned() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User caller = new User();
        caller.setId(1L);
        caller.setFirstName("Alice");
        User callee = new User();
        callee.setId(2L);
        callee.setFirstName("Bob");
        User existingCaller = new User();
        existingCaller.setId(3L);
        existingCaller.setFirstName("Charlie");

        GoToGuyPair existing = new GoToGuyPair();
        existing.setCaller(existingCaller);
        existing.setCallee(callee);

        assertDoesNotThrow(() -> view.showValidationError(caller, callee, List.of(existing)));
    }

    @Test
    void showValidationErrorFallsBackToGenericMessage() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User caller = new User();
        caller.setId(1L);
        caller.setFirstName("Alice");
        User callee = new User();
        callee.setId(2L);
        callee.setFirstName("Bob");

        // No existing pairs, caller != callee -> falls through to "Invalid pair configuration"
        assertDoesNotThrow(() -> view.showValidationError(caller, callee, List.of()));
    }

    // =============================================
    // Member grid deactivate/reactivate tests
    // =============================================

    @Test
    void memberGridConstructionWithActiveNonSelfUserCoversDeactivateBranch() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setFirstName("Admin");
        currentUser.setActive(true);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setFirstName("Bob");
        otherUser.setEmail("bob@example.com");
        otherUser.setActive(true);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userService.getAllUsers()).thenReturn(List.of(currentUser, otherUser));

        // Constructing the view exercises the component column lambda which creates
        // Deactivate buttons for active non-self users
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        // Verify the grid was populated with users
        verify(userService).getAllUsers();
        verify(userService).getCurrentUser();
    }

    @Test
    void memberGridConstructionWithInactiveUserCoversReactivateBranch() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setFirstName("Admin");
        currentUser.setActive(true);

        User inactiveUser = new User();
        inactiveUser.setId(2L);
        inactiveUser.setFirstName("Bob");
        inactiveUser.setEmail("bob@example.com");
        inactiveUser.setActive(false);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userService.getAllUsers()).thenReturn(List.of(currentUser, inactiveUser));

        // Constructing the view exercises the component column lambda which creates
        // Reactivate buttons for inactive non-self users
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        verify(userService).getAllUsers();
    }

    @Test
    void memberGridConstructionWithSelfUserCoversSelfBranch() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setFirstName("Admin");
        currentUser.setActive(true);

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userService.getAllUsers()).thenReturn(List.of(currentUser));

        // Constructing the view with only the self user covers the isSelf branch
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        verify(userService).getCurrentUser();
    }

    @Test
    void memberGridNameColumnRendersFullName() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User userWithName = new User();
        userWithName.setId(2L);
        userWithName.setFirstName("Jane");
        userWithName.setLastName("Doe");
        userWithName.setEmail("jane@example.com");
        userWithName.setActive(true);

        when(userService.getCurrentUser()).thenReturn(null);
        when(userService.getAllUsers()).thenReturn(List.of(userWithName));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        // View construction exercises the name column lambda with first + last name
        verify(userService).getAllUsers();
    }

    @Test
    void memberGridNameColumnRendersNotYetLoggedIn() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User userNoName = new User();
        userNoName.setId(3L);
        userNoName.setEmail("nologin@example.com");
        userNoName.setActive(true);

        when(userService.getCurrentUser()).thenReturn(null);
        when(userService.getAllUsers()).thenReturn(List.of(userNoName));

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        // View construction exercises the name column lambda with null first/last name
        verify(userService).getAllUsers();
    }

    @Test
    void refreshMembersGridCallsGetAllUsers() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        Grid<User> grid = new Grid<>();
        view.refreshMembersGrid(grid);

        // Called once during construction (createMembersTabContent) and once during explicit refresh
        verify(userService, org.mockito.Mockito.atLeast(2)).getAllUsers();
    }

    // =============================================
    // GTG set creation success path tests
    // =============================================

    @Test
    void createNewGTGSetSuccessShowsContentSectionAndAddPairButton() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        Button addPairButton = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));

        // Before clicking
        assertTrue(createButton.isEnabled());
        assertFalse(addPairButton.isEnabled());

        // Click create
        createButton.click();

        // After clicking -- create disabled, add pair enabled
        assertFalse(createButton.isEnabled());
        assertTrue(addPairButton.isEnabled());
        assertTrue(createButton.hasClassName("disabled"));
    }

    // =============================================
    // formatMemberName tests
    // =============================================

    @Test
    void formatMemberNameReturnsFirstAndLastName() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        assertEquals("Jane Doe", view.formatMemberName(user));
    }

    @Test
    void formatMemberNameReturnsFirstNameOnlyWhenLastNull() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        user.setFirstName("Jane");
        assertEquals("Jane", view.formatMemberName(user));
    }

    @Test
    void formatMemberNameReturnsNotYetLoggedInWhenNoName() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        assertEquals("(not yet logged in)", view.formatMemberName(user));
    }

    // =============================================
    // formatMemberStatus tests
    // =============================================

    @Test
    void formatMemberStatusReturnsActiveForActiveUser() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        user.setActive(true);
        assertEquals("Active", view.formatMemberStatus(user));
    }

    @Test
    void formatMemberStatusReturnsInactiveForInactiveUser() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User user = new User();
        user.setActive(false);
        assertEquals("Inactive", view.formatMemberStatus(user));
    }

    // =============================================
    // createMemberActionComponent tests
    // =============================================

    @Test
    void createMemberActionComponentReturnsSelfSpan() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User self = new User();
        self.setId(1L);
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        com.vaadin.flow.component.Component result = view.createMemberActionComponent(self, self, grid);
        assertInstanceOf(com.vaadin.flow.component.html.Span.class, result);
    }

    @Test
    void createMemberActionComponentReturnsDeactivateButtonForActiveNonSelf() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User currentUser = new User(); currentUser.setId(1L);
        User other = new User(); other.setId(2L); other.setActive(true);
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        com.vaadin.flow.component.Component result = view.createMemberActionComponent(other, currentUser, grid);
        assertInstanceOf(Button.class, result);
        assertEquals("Deactivate", ((Button) result).getText());
    }

    @Test
    void createMemberActionComponentReturnsReactivateButtonForInactiveNonSelf() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User currentUser = new User(); currentUser.setId(1L);
        User other = new User(); other.setId(2L); other.setActive(false);
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        com.vaadin.flow.component.Component result = view.createMemberActionComponent(other, currentUser, grid);
        assertInstanceOf(Button.class, result);
        assertEquals("Reactivate", ((Button) result).getText());
    }

    @Test
    void createMemberActionComponentNullCurrentUserNotSelf() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User other = new User(); other.setId(2L); other.setActive(true);
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        com.vaadin.flow.component.Component result = view.createMemberActionComponent(other, null, grid);
        assertInstanceOf(Button.class, result);
        assertEquals("Deactivate", ((Button) result).getText());
    }

    @Test
    void createMemberActionComponentDeactivateButtonClickCallsService() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User currentUser = new User(); currentUser.setId(1L);
        User other = new User(); other.setId(2L); other.setActive(true); other.setFirstName("Bob");
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        try (org.mockito.MockedStatic<AppNotification> mocked = mockStatic(AppNotification.class)) {
            mocked.when(() -> AppNotification.success(anyString())).thenAnswer(inv -> null);
            Button btn = (Button) view.createMemberActionComponent(other, currentUser, grid);
            btn.click();
            verify(userService).deactivateUser(other);
        }
    }

    @Test
    void createMemberActionComponentReactivateButtonClickCallsService() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        User currentUser = new User(); currentUser.setId(1L);
        User other = new User(); other.setId(2L); other.setActive(false); other.setFirstName("Bob");
        com.vaadin.flow.component.grid.Grid<User> grid = new com.vaadin.flow.component.grid.Grid<>();

        try (org.mockito.MockedStatic<AppNotification> mocked = mockStatic(AppNotification.class)) {
            mocked.when(() -> AppNotification.success(anyString())).thenAnswer(inv -> null);
            Button btn = (Button) view.createMemberActionComponent(other, currentUser, grid);
            btn.click();
            verify(userService).reactivateUser(other);
        }
    }

    private GoToGuySet sampleSet() {
        User caller = new User();
        caller.setId(1L);
        caller.setFirstName("Caller");
        User callee = new User();
        callee.setId(2L);
        callee.setFirstName("Callee");

        GoToGuyPair pair = new GoToGuyPair();
        pair.setCaller(caller);
        pair.setCallee(callee);

        GoToGuySet set = new GoToGuySet();
        set.setGoToGuyPairs(List.of(pair));
        return set;
    }

    // ========================
    // Integrations Tab Tests
    // ========================

    @Test
    void integrationsTabContentLoadsWithNoExistingSettings() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());
        when(groupSettingsService.getSettings()).thenReturn(new GroupSettings());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout integrationsTab = view.createIntegrationsTabContent();
        assertNotNull(integrationsTab);
    }

    @Test
    void integrationsTabContentLoadsWithExistingSettings() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());
        GroupSettings settings = new GroupSettings();
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        settings.setSlackEnabled(true);
        when(groupSettingsService.getSettings()).thenReturn(settings);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);

        VerticalLayout integrationsTab = view.createIntegrationsTabContent();
        assertNotNull(integrationsTab);
    }

    @Test
    void integrationsTabSaveButtonCallsGroupSettingsService() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());
        GroupSettings settings = new GroupSettings();
        when(groupSettingsService.getSettings()).thenReturn(settings);
        when(groupSettingsService.save(any(GroupSettings.class))).thenReturn(settings);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService, groupSettingsService);
        ui.add(view);

        VerticalLayout integrationsTab = view.createIntegrationsTabContent();
        Button saveBtn = firstComponent(integrationsTab, Button.class, b -> "Save".equals(b.getText()));
        saveBtn.click();

        verify(groupSettingsService).save(any(GroupSettings.class));
    }

    private <T extends Component> boolean anyComponent(Component root, Class<T> type, Predicate<T> predicate) {
        return findComponents(root, type).stream().anyMatch(predicate);
    }

    private <T extends Component> T firstComponent(Component root, Class<T> type, Predicate<T> predicate) {
        return findComponents(root, type).stream().filter(predicate).findFirst().orElseThrow();
    }

    private <T extends Component> List<T> findComponents(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        ArrayDeque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component current = stack.pop();
            if (type.isInstance(current)) {
                matches.add(type.cast(current));
            }
            current.getChildren().forEach(stack::push);
        }
        assertNotNull(matches);
        return matches;
    }

}
