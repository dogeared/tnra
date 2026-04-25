package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlackNotificationServiceImplTest {

    private GroupSettingsService groupSettingsService;
    private SlackNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        groupSettingsService = mock(GroupSettingsService.class);
        service = new SlackNotificationServiceImpl(groupSettingsService, "https://tnra.example.com");
    }

    // --- constructor ---

    @Test
    void constructorStripsTrailingSlash() {
        SlackNotificationServiceImpl svc = new SlackNotificationServiceImpl(groupSettingsService, "https://example.com/");
        Post post = createPost("Alice", "Smith", 1L);
        String msg = svc.buildMessage(post);
        // URL in message should not have double slash
        assertTrue(msg.contains("https://example.com/posts/1"));
        assertFalse(msg.contains("//posts/"));
    }

    // --- buildMessage ---

    @Test
    void buildMessage_includesFullNameStartFinishAndLink() {
        Post post = createPost("Alice", "Smith", 99L);
        post.setStart(new Date(0));
        post.setFinish(new Date(60_000));

        String msg = service.buildMessage(post);

        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("finished a post"));
        assertTrue(msg.contains("Started:"));
        assertTrue(msg.contains("Finished:"));
        assertTrue(msg.contains("https://tnra.example.com/posts/99"));
    }

    @Test
    void buildMessage_usesFirstNameOnlyWhenLastNameNull() {
        Post post = createPost("Bob", null, 1L);
        String msg = service.buildMessage(post);
        assertTrue(msg.startsWith("Bob "));
    }

    @Test
    void buildMessage_usesEmailWhenNamesNull() {
        User user = new User(null, null, "anon@example.com");
        Post post = new Post();
        post.setUser(user);
        post.setId(2L);

        String msg = service.buildMessage(post);
        assertTrue(msg.contains("anon@example.com"));
    }

    @Test
    void buildMessage_usesSomeoneWhenUserNull() {
        Post post = new Post();
        post.setUser(null);
        post.setId(3L);

        String msg = service.buildMessage(post);
        assertTrue(msg.startsWith("Someone "));
    }

    @Test
    void buildMessage_handlesNullStartAndFinish() {
        Post post = createPost("Carol", "Jones", 5L);
        post.setStart(null);
        post.setFinish(null);

        String msg = service.buildMessage(post);
        assertTrue(msg.contains("Started: unknown"));
        assertTrue(msg.contains("Finished: unknown"));
    }

    @Test
    void buildMessage_handlesNullPostId() {
        Post post = createPost("Dave", "Kim", null);
        String msg = service.buildMessage(post);
        assertTrue(msg.contains("https://tnra.example.com/posts/"));
    }

    @Test
    void buildMessage_usesSomeoneWhenUserHasAllNullFields() {
        User user = new User(null, null, null);
        Post post = new Post();
        post.setUser(user);
        post.setId(7L);

        String msg = service.buildMessage(post);
        assertTrue(msg.startsWith("Someone "));
    }

    // --- sendActivityNotification (early-exit paths, no real HTTP) ---

    @Test
    void sendActivityNotification_skipsWhenDisabled() {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(false);
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        // Should not throw; no HTTP call made (would fail immediately if attempted with this URL)
        service.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
    }

    @Test
    void sendActivityNotification_skipsWhenWebhookUrlNull() {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl(null);
        when(groupSettingsService.getSettings()).thenReturn(settings);

        service.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
    }

    @Test
    void sendActivityNotification_skipsWhenWebhookUrlBlank() {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl("   ");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        service.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
    }

    @Test
    void sendActivityNotification_postsWhenEnabledAndUrlSet() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        SlackNotificationServiceImpl spy = spy(service);
        doNothing().when(spy).doPost(anyString(), anyString());

        spy.sendActivityNotification(createPost("Alice", "Smith", 10L));

        verify(spy).doPost(eq("https://hooks.slack.com/test"), contains("Alice Smith"));
    }

    @Test
    void sendActivityNotification_logsErrorOnIOException() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        SlackNotificationServiceImpl spy = spy(service);
        doThrow(new IOException("timeout")).when(spy).doPost(anyString(), anyString());

        // should not throw — error is logged
        assertDoesNotThrow(() -> spy.sendActivityNotification(createPost("Bob", "Jones", 11L)));
    }

    private Post createPost(String firstName, String lastName, Long id) {
        User user = new User(firstName, lastName, "test@example.com");
        Post post = new Post();
        post.setUser(user);
        post.setId(id);
        return post;
    }
}
