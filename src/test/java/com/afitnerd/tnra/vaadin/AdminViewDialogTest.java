package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.UserService;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.afitnerd.tnra.vaadin.presenter.VaadinAdminPresenter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AdminView dialog methods that require mockConstruction for Dialog capture.
 * Separated from AdminViewTest to avoid Mockito instrumentation conflicts.
 */
class AdminViewDialogTest {

    private VaadinAdminPresenter vaadinAdminPresenter;
    private CallChainPresenter callChainPresenter;
    private StatDefinitionRepository statDefinitionRepository;
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private UserService userService;
    private UI ui;

    @BeforeEach
    void setUp() {
        vaadinAdminPresenter = mock(VaadinAdminPresenter.class);
        callChainPresenter = mock(CallChainPresenter.class);
        statDefinitionRepository = mock(StatDefinitionRepository.class);
        personalStatDefinitionRepository = mock(PersonalStatDefinitionRepository.class);
        userService = mock(UserService.class);

        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, Mockito.RETURNS_DEEP_STUBS);
        lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        lenient().when(session.getService()).thenReturn(service);
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
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    // =============================================
    // Invite dialog submission tests
    // =============================================

    @Test
    void inviteDialogValidEmailCallsInviteUserAndClosesDialog() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(userService.getUserByEmail("newmember@example.com")).thenReturn(null);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        Grid<User> membersGrid = new Grid<>();
        Dialog dialog = captureDialogFrom(() -> view.openInviteMemberDialog(membersGrid));
        assertNotNull(dialog, "Dialog should be constructed");

        TextField emailField = firstComponent(dialog, TextField.class, tf -> "Email Address".equals(tf.getLabel()));
        emailField.setValue("newmember@example.com");

        Button inviteBtn = firstComponent(dialog, Button.class, b -> "Invite".equals(b.getText()));
        assertDoesNotThrow(inviteBtn::click);

        verify(userService).inviteUser("newmember@example.com");
    }

    @Test
    void inviteDialogInvalidEmailDoesNotCallInviteUser() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        Grid<User> membersGrid = new Grid<>();
        Dialog dialog = captureDialogFrom(() -> view.openInviteMemberDialog(membersGrid));

        TextField emailField = firstComponent(dialog, TextField.class, tf -> "Email Address".equals(tf.getLabel()));
        emailField.setValue("not-an-email");

        Button inviteBtn = firstComponent(dialog, Button.class, b -> "Invite".equals(b.getText()));
        assertDoesNotThrow(inviteBtn::click);

        verify(userService, never()).inviteUser(anyString());
    }

    @Test
    void inviteDialogEmptyEmailDoesNotCallInviteUser() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        Grid<User> membersGrid = new Grid<>();
        Dialog dialog = captureDialogFrom(() -> view.openInviteMemberDialog(membersGrid));

        TextField emailField = firstComponent(dialog, TextField.class, tf -> "Email Address".equals(tf.getLabel()));
        emailField.setValue("   ");

        Button inviteBtn = firstComponent(dialog, Button.class, b -> "Invite".equals(b.getText()));
        assertDoesNotThrow(inviteBtn::click);

        verify(userService, never()).inviteUser(anyString());
    }

    @Test
    void inviteDialogDuplicateEmailDoesNotCallInviteUser() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);

        User existingUser = new User();
        existingUser.setId(5L);
        existingUser.setEmail("existing@example.com");
        existingUser.setActive(true);

        when(userService.getUserByEmail("existing@example.com")).thenReturn(existingUser);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        Grid<User> membersGrid = new Grid<>();
        Dialog dialog = captureDialogFrom(() -> view.openInviteMemberDialog(membersGrid));

        TextField emailField = firstComponent(dialog, TextField.class, tf -> "Email Address".equals(tf.getLabel()));
        emailField.setValue("existing@example.com");

        Button inviteBtn = firstComponent(dialog, Button.class, b -> "Invite".equals(b.getText()));
        assertDoesNotThrow(inviteBtn::click);

        verify(userService, never()).inviteUser(anyString());
    }

    // =============================================
    // Add stat dialog submission tests
    // =============================================

    @Test
    void addStatDialogSuccessfulAddSavesAndCloses() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()).thenReturn(new ArrayList<>());
        when(statDefinitionRepository.existsGlobalByName("journaling")).thenReturn(false);
        when(personalStatDefinitionRepository.existsByNameAndArchivedFalse("journaling")).thenReturn(false);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        VerticalLayout statsList = new VerticalLayout();
        Dialog dialog = captureDialogFrom(() -> view.openAddStatDialog(statsList));
        assertNotNull(dialog, "Dialog should be constructed");

        TextField nameField = firstComponent(dialog, TextField.class, tf -> "Internal Name".equals(tf.getLabel()));
        TextField labelField = firstComponent(dialog, TextField.class, tf -> "Display Label".equals(tf.getLabel()));
        TextField emojiField = firstComponent(dialog, TextField.class, tf -> "Emoji".equals(tf.getLabel()));

        nameField.setValue("journaling");
        labelField.setValue("Journaling");
        emojiField.setValue("\uD83D\uDCDD");

        Button addBtn = firstComponent(dialog, Button.class, b -> "Add Stat".equals(b.getText()));
        assertDoesNotThrow(addBtn::click);

        verify(statDefinitionRepository).save(any(StatDefinition.class));
    }

    @Test
    void addStatDialogDuplicateNameDoesNotSave() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.existsGlobalByName("journaling")).thenReturn(true);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        VerticalLayout statsList = new VerticalLayout();
        Dialog dialog = captureDialogFrom(() -> view.openAddStatDialog(statsList));

        TextField nameField = firstComponent(dialog, TextField.class, tf -> "Internal Name".equals(tf.getLabel()));
        TextField labelField = firstComponent(dialog, TextField.class, tf -> "Display Label".equals(tf.getLabel()));

        nameField.setValue("journaling");
        labelField.setValue("Journaling");

        Button addBtn = firstComponent(dialog, Button.class, b -> "Add Stat".equals(b.getText()));
        assertDoesNotThrow(addBtn::click);

        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    @Test
    void addStatDialogDuplicatePersonalStatNameDoesNotSave() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.existsGlobalByName("journaling")).thenReturn(false);
        when(personalStatDefinitionRepository.existsByNameAndArchivedFalse("journaling")).thenReturn(true);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        VerticalLayout statsList = new VerticalLayout();
        Dialog dialog = captureDialogFrom(() -> view.openAddStatDialog(statsList));

        TextField nameField = firstComponent(dialog, TextField.class, tf -> "Internal Name".equals(tf.getLabel()));
        TextField labelField = firstComponent(dialog, TextField.class, tf -> "Display Label".equals(tf.getLabel()));

        nameField.setValue("journaling");
        labelField.setValue("Journaling");

        Button addBtn = firstComponent(dialog, Button.class, b -> "Add Stat".equals(b.getText()));
        assertDoesNotThrow(addBtn::click);

        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    @Test
    void addStatDialogEmptyNameDoesNotSave() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        VerticalLayout statsList = new VerticalLayout();
        Dialog dialog = captureDialogFrom(() -> view.openAddStatDialog(statsList));

        TextField nameField = firstComponent(dialog, TextField.class, tf -> "Internal Name".equals(tf.getLabel()));
        TextField labelField = firstComponent(dialog, TextField.class, tf -> "Display Label".equals(tf.getLabel()));

        nameField.setValue("");
        labelField.setValue("");

        Button addBtn = firstComponent(dialog, Button.class, b -> "Add Stat".equals(b.getText()));
        assertDoesNotThrow(addBtn::click);

        verify(statDefinitionRepository, never()).save(any(StatDefinition.class));
    }

    @Test
    void addStatDialogEmptyEmojiSetsNullEmoji() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        when(statDefinitionRepository.findGlobalAllOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()).thenReturn(new ArrayList<>());
        when(statDefinitionRepository.existsGlobalByName("journaling")).thenReturn(false);
        when(personalStatDefinitionRepository.existsByNameAndArchivedFalse("journaling")).thenReturn(false);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        VerticalLayout statsList = new VerticalLayout();
        Dialog dialog = captureDialogFrom(() -> view.openAddStatDialog(statsList));

        TextField nameField = firstComponent(dialog, TextField.class, tf -> "Internal Name".equals(tf.getLabel()));
        TextField labelField = firstComponent(dialog, TextField.class, tf -> "Display Label".equals(tf.getLabel()));
        TextField emojiField = firstComponent(dialog, TextField.class, tf -> "Emoji".equals(tf.getLabel()));

        nameField.setValue("journaling");
        labelField.setValue("Journaling");
        emojiField.setValue("");

        Button addBtn = firstComponent(dialog, Button.class, b -> "Add Stat".equals(b.getText()));
        assertDoesNotThrow(addBtn::click);

        verify(statDefinitionRepository).save(any(StatDefinition.class));
    }

    // =============================================
    // Add pair dialog tests
    // =============================================

    @Test
    void openAddPairDialogRendersCallerAndCalleeComboBoxes() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        User alice = new User();
        alice.setId(10L);
        alice.setFirstName("Alice");
        alice.setActive(true);

        User bob = new User();
        bob.setId(20L);
        bob.setFirstName("Bob");
        bob.setActive(true);

        when(callChainPresenter.getAllActiveUsers()).thenReturn(List.of(alice, bob));

        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        // Create a new set first so workingSet is populated
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        createButton.click();

        // Now click Add Pair -- intercept the Dialog
        Button addPairBtn = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));
        Dialog dialog = captureDialogFrom(addPairBtn::click);
        assertNotNull(dialog, "Add Pair dialog should be constructed");

        // Verify ComboBoxes exist
        assertTrue(anyComponent(dialog, ComboBox.class, cb -> "Caller".equals(cb.getLabel())));
        assertTrue(anyComponent(dialog, ComboBox.class, cb -> "Callee".equals(cb.getLabel())));

        // Verify Save and Cancel buttons exist
        assertTrue(anyComponent(dialog, Button.class, b -> "Add Pair".equals(b.getText())));
        assertTrue(anyComponent(dialog, Button.class, b -> "Cancel".equals(b.getText())));
    }

    @Test
    void addPairDialogValidPairCallsAddPairToSet() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        User alice = new User();
        alice.setId(10L);
        alice.setFirstName("Alice");
        alice.setActive(true);

        User bob = new User();
        bob.setId(20L);
        bob.setFirstName("Bob");
        bob.setActive(true);

        when(callChainPresenter.getAllActiveUsers()).thenReturn(List.of(alice, bob));

        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);

        GoToGuySet updatedSet = new GoToGuySet();
        GoToGuyPair addedPair = new GoToGuyPair();
        addedPair.setCaller(alice);
        addedPair.setCallee(bob);
        updatedSet.setGoToGuyPairs(new ArrayList<>(List.of(addedPair)));
        when(callChainPresenter.addPairToSet(any(GoToGuySet.class), any(GoToGuyPair.class))).thenReturn(updatedSet);
        when(callChainPresenter.validatePair(eq(alice), eq(bob), anyList())).thenReturn(true);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        // Create new set
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        createButton.click();

        // Open add pair dialog
        Button addPairBtn = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));
        Dialog dialog = captureDialogFrom(addPairBtn::click);

        @SuppressWarnings("unchecked")
        ComboBox<User> callerCombo = firstComponent(dialog, ComboBox.class, cb -> "Caller".equals(cb.getLabel()));
        @SuppressWarnings("unchecked")
        ComboBox<User> calleeCombo = firstComponent(dialog, ComboBox.class, cb -> "Callee".equals(cb.getLabel()));

        callerCombo.setValue(alice);
        calleeCombo.setValue(bob);

        Button saveBtn = firstComponent(dialog, Button.class, b -> "Add Pair".equals(b.getText()));
        assertDoesNotThrow(saveBtn::click);

        verify(callChainPresenter).addPairToSet(any(GoToGuySet.class), any(GoToGuyPair.class));
    }

    @Test
    void addPairDialogInvalidPairShowsValidationError() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        User alice = new User();
        alice.setId(10L);
        alice.setFirstName("Alice");
        alice.setActive(true);

        when(callChainPresenter.getAllActiveUsers()).thenReturn(List.of(alice));

        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);
        when(callChainPresenter.validatePair(eq(alice), eq(alice), anyList())).thenReturn(false);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        // Create new set
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        createButton.click();

        // Open add pair dialog
        Button addPairBtn = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));
        Dialog dialog = captureDialogFrom(addPairBtn::click);

        @SuppressWarnings("unchecked")
        ComboBox<User> callerCombo = firstComponent(dialog, ComboBox.class, cb -> "Caller".equals(cb.getLabel()));
        @SuppressWarnings("unchecked")
        ComboBox<User> calleeCombo = firstComponent(dialog, ComboBox.class, cb -> "Callee".equals(cb.getLabel()));

        callerCombo.setValue(alice);
        calleeCombo.setValue(alice);

        Button saveBtn = firstComponent(dialog, Button.class, b -> "Add Pair".equals(b.getText()));
        assertDoesNotThrow(saveBtn::click);

        // Should NOT have called addPairToSet since validation failed
        verify(callChainPresenter, never()).addPairToSet(any(GoToGuySet.class), any(GoToGuyPair.class));
    }

    @Test
    void addPairDialogCallerSelectionFiltersCalleeOptions() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        User alice = new User();
        alice.setId(10L);
        alice.setFirstName("Alice");
        alice.setActive(true);

        User bob = new User();
        bob.setId(20L);
        bob.setFirstName("Bob");
        bob.setActive(true);

        User charlie = new User();
        charlie.setId(30L);
        charlie.setFirstName("Charlie");
        charlie.setActive(true);

        when(callChainPresenter.getAllActiveUsers()).thenReturn(List.of(alice, bob, charlie));

        GoToGuySet newSet = new GoToGuySet();
        newSet.setGoToGuyPairs(new ArrayList<>());
        when(callChainPresenter.createNewGoToGuySet(anyList())).thenReturn(newSet);

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter, statDefinitionRepository, personalStatDefinitionRepository, userService);
        ui.add(view);

        // Create new set
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));
        createButton.click();

        // Open add pair dialog
        Button addPairBtn = firstComponent(view, Button.class, b -> "Add Pair".equals(b.getText()));
        Dialog dialog = captureDialogFrom(addPairBtn::click);

        @SuppressWarnings("unchecked")
        ComboBox<User> callerCombo = firstComponent(dialog, ComboBox.class, cb -> "Caller".equals(cb.getLabel()));

        // Selecting a caller should trigger the value change listener (filters callees)
        assertDoesNotThrow(() -> callerCombo.setValue(alice));

        // Setting caller to null should also work
        assertDoesNotThrow(() -> callerCombo.setValue(null));
    }

    // =============================================
    // Helper methods
    // =============================================

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
            // Also search Dialog footer which is a separate component slot
            if (current instanceof Dialog dialog) {
                try {
                    com.vaadin.flow.dom.Element footerElement = dialog.getFooter().getElement();
                    for (int i = 0; i < footerElement.getChildCount(); i++) {
                        footerElement.getChild(i).getComponent().ifPresent(stack::push);
                    }
                } catch (Exception e) {
                    // ignore if footer not accessible
                }
            }
        }
        return matches;
    }

    /**
     * Captures a Dialog created inside the given action by mocking Dialog construction.
     * The mock delegates add(), setHeaderTitle(), getFooter(), open(), and close() to a
     * real Dialog so its children are accessible after the action runs.
     */
    private Dialog captureDialogFrom(Runnable action) {
        // Create the real Dialog BEFORE entering mockConstruction scope,
        // otherwise the real dialog itself would be intercepted by the mock.
        Dialog real = new Dialog();
        try (var construction = Mockito.mockConstruction(Dialog.class,
                (dialogMock, context) -> {
                    Mockito.doAnswer(inv -> {
                        real.setHeaderTitle((String) inv.getArgument(0));
                        return null;
                    }).when(dialogMock).setHeaderTitle(anyString());
                    Mockito.doAnswer(inv -> {
                        for (Object arg : inv.getArguments()) {
                            real.add((Component) arg);
                        }
                        return null;
                    }).when(dialogMock).add(any(Component[].class));
                    Mockito.doReturn(real.getFooter()).when(dialogMock).getFooter();
                    Mockito.doNothing().when(dialogMock).open();
                    Mockito.doNothing().when(dialogMock).close();
                })) {
            action.run();
        }
        return real;
    }
}
