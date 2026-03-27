package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.CallChainPresenter;
import com.afitnerd.tnra.vaadin.presenter.VaadinAdminPresenter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.VaadinRequest;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminViewTest {

    @Mock
    private VaadinAdminPresenter vaadinAdminPresenter;

    @Mock
    private CallChainPresenter callChainPresenter;

    @BeforeEach
    void setUp() {
        UI ui = new UI();
        VaadinSession session = mock(VaadinSession.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        org.mockito.Mockito.lenient().when(session.hasLock()).thenReturn(true);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        when(vaadinAdminPresenter.getGitTag()).thenReturn("v1.0.0");
        when(vaadinAdminPresenter.getGitCommitId()).thenReturn("abc123");
        when(vaadinAdminPresenter.getGitBranch()).thenReturn("main");
        when(vaadinAdminPresenter.getSpringBootVersion()).thenReturn("3.x");
        when(vaadinAdminPresenter.getVaadinVersion()).thenReturn("24.x");
        when(vaadinAdminPresenter.getJavaVersion()).thenReturn("21");
        when(vaadinAdminPresenter.getBuildTime()).thenReturn("2026-03-13T17:00Z");
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void buildsHeaderTabsAndCurrentSetGrid() {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(sampleSet());

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter);

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

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter);
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

        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter);
        Button createButton = firstComponent(view, Button.class, b -> "Create New Go To Guy Set".equals(b.getText()));

        assertDoesNotThrow(createButton::click);
        assertTrue(createButton.isEnabled());
    }

    @Test
    void getUserDisplayNameHandlesAllFallbacks() throws Exception {
        when(callChainPresenter.getCurrentGoToGuySet()).thenReturn(null);
        AdminView view = new AdminView(vaadinAdminPresenter, callChainPresenter);

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
        }
        assertNotNull(matches);
        return matches;
    }
}
