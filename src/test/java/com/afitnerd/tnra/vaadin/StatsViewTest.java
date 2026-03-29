package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.vaadin.presenter.VaadinPostPresenter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsViewTest {

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
    private List<StatDefinition> defaultStatDefs;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Create default stat definitions
        StatDefinition exerciseDef = new StatDefinition("exercise", "Exercise", "💪", 0);
        exerciseDef.setId(1L);
        StatDefinition meditateDef = new StatDefinition("meditate", "Meditate", "🧘", 1);
        meditateDef.setId(2L);
        StatDefinition prayDef = new StatDefinition("pray", "Pray", "🙏", 2);
        prayDef.setId(3L);
        defaultStatDefs = List.of(exerciseDef, meditateDef, prayDef);

        inProgressPost = new Post();
        inProgressPost.setId(1L);
        inProgressPost.setUser(testUser);
        inProgressPost.setState(PostState.IN_PROGRESS);
        inProgressPost.setStart(new Date());
        // Set stat values on the post
        inProgressPost.setStatValue(exerciseDef, 30);
        inProgressPost.setStatValue(meditateDef, 20);
        inProgressPost.setStatValue(prayDef, 15);

        lenient().when(vaadinPostPresenter.initializeUser()).thenReturn(testUser);
        lenient().when(vaadinPostPresenter.getActiveGlobalStatDefinitions()).thenReturn(defaultStatDefs);
        lenient().when(vaadinPostPresenter.getActivePersonalStatDefinitions(testUser)).thenReturn(List.of());
    }

    @Test
    void testCreateEmbeddedStatsView() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);

        assertNotNull(embeddedStatsView);
        assertEquals(2, embeddedStatsView.getComponentCount());
    }

    @Test
    void testStatsViewWithInProgressPost() {
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.of(inProgressPost));

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            StatsView statsView = new StatsView(vaadinPostPresenter);
            statsView.afterNavigation(mockAfterNavigationEvent());

            assertNotNull(statsView);
            assertTrue(statsView.getComponentCount() > 0);
        }
    }

    @Test
    void testStatsViewWithNoInProgressPost_CreatesNewPost() {
        lenient().when(vaadinPostPresenter.getOptionalInProgressPost(testUser)).thenReturn(Optional.empty());
        lenient().when(vaadinPostPresenter.startPost(testUser)).thenReturn(inProgressPost);

        try (MockedStatic<UI> mockedUI = mockStatic(UI.class)) {
            setupUIMocks("America/New_York");
            mockedUI.when(UI::getCurrent).thenReturn(mockUI);

            StatsView statsView = new StatsView(vaadinPostPresenter);
            statsView.afterNavigation(mockAfterNavigationEvent());

            verify(vaadinPostPresenter).startPost(testUser);
        }
    }

    @Test
    void testSetPost() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(inProgressPost);

        assertTrue(embeddedStatsView.areAllStatsSet());
    }

    @Test
    void testSetPostWithNull() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(inProgressPost);
        embeddedStatsView.setPost(null);

        assertFalse(embeddedStatsView.areAllStatsSet());
    }

    @Test
    void testAreAllStatsSetWithCompleteStats() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(inProgressPost);

        assertTrue(embeddedStatsView.areAllStatsSet());
    }

    @Test
    void testAreAllStatsSetWithIncompleteStats() {
        Post incompletePost = new Post();
        StatDefinition exerciseDef = defaultStatDefs.get(0);
        incompletePost.setStatValue(exerciseDef, 30);
        // meditate and pray are missing

        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(incompletePost);

        assertFalse(embeddedStatsView.areAllStatsSet());
    }

    @Test
    void testAreAllStatsSetWithNullPost() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(null);

        assertFalse(embeddedStatsView.areAllStatsSet());
    }

    @Test
    void testSetReadOnly() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(inProgressPost);

        assertDoesNotThrow(() -> embeddedStatsView.setReadOnly(false));
        assertDoesNotThrow(() -> embeddedStatsView.setReadOnly(true));
    }

    @Test
    void testRefreshStats() {
        StatsView embeddedStatsView = StatsView.createEmbedded(vaadinPostPresenter, testUser);
        embeddedStatsView.setPost(inProgressPost);
        embeddedStatsView.refreshStats();

        assertTrue(embeddedStatsView.areAllStatsSet());
    }

    private void setupUIMocks(String timeZoneId) {
        lenient().when(mockUI.getSession()).thenReturn(mockSession);
        lenient().when(mockExtendedClientDetails.getTimeZoneId()).thenReturn(timeZoneId);
        lenient().when(mockSession.getAttribute(ExtendedClientDetails.class)).thenReturn(mockExtendedClientDetails);
    }

    private com.vaadin.flow.router.AfterNavigationEvent mockAfterNavigationEvent() {
        return mock(com.vaadin.flow.router.AfterNavigationEvent.class);
    }
}
