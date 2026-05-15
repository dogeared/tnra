package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlackNotificationServiceImplTest {

    private GroupSettingsService groupSettingsService;
    private PostTokenService postTokenService;
    private SlackPostBodyRenderer slackPostBodyRenderer;
    private SlackStatsRenderer slackStatsRenderer;
    private SlackNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        groupSettingsService = mock(GroupSettingsService.class);
        postTokenService = mock(PostTokenService.class);
        slackPostBodyRenderer = new SlackPostBodyRenderer();
        slackStatsRenderer = new SlackStatsRenderer();
        // Default stub: return a placeholder token for any Long ID
        when(postTokenService.encode(anyLong())).thenReturn("token");
        service = new SlackNotificationServiceImpl(
            groupSettingsService, postTokenService,
            slackPostBodyRenderer, slackStatsRenderer,
            "https://tnra.example.com");
    }

    /** No-op settings used by the activity-line-only tests below. */
    private GroupSettings emptySettings() {
        return new GroupSettings();
    }

    // --- constructor ---

    @Test
    void constructorStripsTrailingSlash() {
        when(postTokenService.encode(1L)).thenReturn("token1");
        SlackNotificationServiceImpl svc = new SlackNotificationServiceImpl(
            groupSettingsService, postTokenService,
            slackPostBodyRenderer, slackStatsRenderer,
            "https://example.com/");
        Post post = createPost("Alice", "Smith", 1L);
        String msg = svc.buildMessage(post, emptySettings());
        // URL in mrkdwn link should not have double slash
        assertTrue(msg.contains("<https://example.com/posts/token1|here>"));
        assertFalse(msg.contains("//posts/"));
    }

    // --- buildMessage ---

    @Test
    void buildMessage_includesFullNameStartFinishAndLink() {
        when(postTokenService.encode(99L)).thenReturn("token99");
        Post post = createPost("Alice", "Smith", 99L);
        post.setStart(new Date(0));
        post.setFinish(new Date(60_000));

        String msg = service.buildMessage(post, emptySettings());

        assertTrue(msg.contains("Alice Smith"));
        assertTrue(msg.contains("finished a post"));
        assertTrue(msg.contains("Started:"));
        assertTrue(msg.contains("Finished:"));
        assertTrue(msg.contains("https://tnra.example.com/posts/token99"));
        // Slack mrkdwn hyperlink: <URL|here>
        assertTrue(msg.contains("<https://tnra.example.com/posts/token99|here>"));
        assertFalse(msg.contains("| View: "), "Should use mrkdwn link format, not plain URL");
    }

    @Test
    void buildMessage_viewLinkFormattedAsMrkdwn() {
        when(postTokenService.encode(7L)).thenReturn("tok7");
        Post post = createPost("Jen", "Lee", 7L);

        String msg = service.buildMessage(post, emptySettings());

        assertTrue(msg.contains("View <https://tnra.example.com/posts/tok7|here>"));
    }

    @Test
    void buildMessage_usesFirstNameOnlyWhenLastNameNull() {
        Post post = createPost("Bob", null, 1L);
        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.startsWith("Bob "));
    }

    @Test
    void buildMessage_usesEmailWhenNamesNull() {
        User user = new User(null, null, "anon@example.com");
        Post post = new Post();
        post.setUser(user);
        post.setId(2L);

        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.contains("anon@example.com"));
    }

    @Test
    void buildMessage_usesSomeoneWhenUserNull() {
        Post post = new Post();
        post.setUser(null);
        post.setId(3L);

        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.startsWith("Someone "));
    }

    @Test
    void buildMessage_handlesNullStartAndFinish() {
        Post post = createPost("Carol", "Jones", 5L);
        post.setStart(null);
        post.setFinish(null);

        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.contains("Started: unknown"));
        assertTrue(msg.contains("Finished: unknown"));
    }

    @Test
    void buildMessage_handlesNullPostId() {
        Post post = createPost("Dave", "Kim", null);
        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.contains("https://tnra.example.com/posts/"));
    }

    @Test
    void buildMessage_escapesSlackMrkdwnInUserName() {
        Post post = createPost("<attacker>", "User", 5L);
        when(postTokenService.encode(5L)).thenReturn("tok5");
        String msg = service.buildMessage(post, emptySettings());
        assertFalse(msg.contains("<attacker>"), "Raw angle brackets should be escaped");
        assertTrue(msg.contains("&lt;attacker&gt;"));
    }

    @Test
    void buildMessage_usesSomeoneWhenUserHasAllNullFields() {
        User user = new User(null, null, null);
        Post post = new Post();
        post.setUser(user);
        post.setId(7L);

        String msg = service.buildMessage(post, emptySettings());
        assertTrue(msg.startsWith("Someone "));
    }

    // --- sendActivityNotification (early-exit paths, no real HTTP) ---

    @Test
    void sendActivityNotification_skipsWhenDisabled() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(false);
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        SlackNotificationServiceImpl spy = spy(service);
        spy.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
        verify(spy, never()).doPost(anyString(), anyString());
    }

    @Test
    void sendActivityNotification_skipsWhenWebhookUrlNull() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl(null);
        when(groupSettingsService.getSettings()).thenReturn(settings);

        SlackNotificationServiceImpl spy = spy(service);
        spy.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
        verify(spy, never()).doPost(anyString(), anyString());
    }

    @Test
    void sendActivityNotification_skipsWhenWebhookUrlBlank() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl("   ");
        when(groupSettingsService.getSettings()).thenReturn(settings);

        SlackNotificationServiceImpl spy = spy(service);
        spy.sendActivityNotification(createPost("Test", "User", 1L));
        verify(groupSettingsService, times(1)).getSettings();
        verify(spy, never()).doPost(anyString(), anyString());
    }

    @Test
    void doPost_rejectsNonSlackUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> service.doPost("https://evil.example.com/webhook", "{\"text\":\"x\"}"));
    }

    @Test
    void doPost_rejectsHttpUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> service.doPost("http://hooks.slack.com/test", "{\"text\":\"x\"}"));
    }

    @Test
    void sendActivityNotification_postsWhenEnabledAndUrlSet() throws Exception {
        GroupSettings settings = new GroupSettings();
        settings.setSlackEnabled(true);
        settings.setSlackWebhookUrl("https://hooks.slack.com/test");
        when(groupSettingsService.getSettings()).thenReturn(settings);
        when(postTokenService.encode(10L)).thenReturn("token10");

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

    // --- publishing matrix (body / stats decision logic) ---

    @Test
    void buildMessage_noEnrichmentWhenMasterOff() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 50L);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(false);
        s.setSlackPublishPostBody(true);
        s.setSlackPublishStats(true);

        String msg = service.buildMessage(post, s);

        assertFalse(msg.contains("*Intro*"), "Body must not appear when master is off");
        assertFalse(msg.contains("*Stats*"), "Stats must not appear when master is off");
    }

    @Test
    void buildMessage_noEnrichmentWhenMasterOnButNothingElseAndUserOptOut() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 51L);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);
        // both sub-overrides off, user defaults off

        String msg = service.buildMessage(post, s);

        assertFalse(msg.contains("*Intro*"));
        assertFalse(msg.contains("*Stats*"));
    }

    @Test
    void buildMessage_bodyOnlyWhenGroupForcesBody() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 52L);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);
        s.setSlackPublishPostBody(true);

        String msg = service.buildMessage(post, s);

        assertTrue(msg.contains("*Intro*"));
        assertFalse(msg.contains("*Stats*"));
    }

    @Test
    void buildMessage_statsOnlyWhenGroupForcesStats() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 53L);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);
        s.setSlackPublishStats(true);

        String msg = service.buildMessage(post, s);

        assertFalse(msg.contains("*Intro*"));
        assertTrue(msg.contains("*Stats*"));
    }

    @Test
    void buildMessage_bodyOnlyWhenUserOptsIn() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 54L);
        post.getUser().setSlackPublishPostBody(true);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);

        String msg = service.buildMessage(post, s);

        assertTrue(msg.contains("*Intro*"));
        assertFalse(msg.contains("*Stats*"));
    }

    @Test
    void buildMessage_statsOnlyWhenUserOptsIn() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 55L);
        post.getUser().setSlackPublishStats(true);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);

        String msg = service.buildMessage(post, s);

        assertFalse(msg.contains("*Intro*"));
        assertTrue(msg.contains("*Stats*"));
    }

    @Test
    void buildMessage_bodyBeforeStatsWhenBothSent() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 56L);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);
        s.setSlackPublishPostBody(true);
        s.setSlackPublishStats(true);

        String msg = service.buildMessage(post, s);

        int introIdx = msg.indexOf("*Intro*");
        int statsIdx = msg.indexOf("*Stats*");
        assertTrue(introIdx > 0, "Body must be present");
        assertTrue(statsIdx > 0, "Stats must be present");
        assertTrue(introIdx < statsIdx, "Body must appear before stats");
    }

    @Test
    void buildMessage_groupOverrideWinsEvenIfUserOptOut() {
        Post post = createPostWithBodyAndStats("Alice", "Smith", 57L);
        post.getUser().setSlackPublishPostBody(false);
        post.getUser().setSlackPublishStats(false);
        GroupSettings s = new GroupSettings();
        s.setSlackPublishPostData(true);
        s.setSlackPublishPostBody(true);
        s.setSlackPublishStats(true);

        String msg = service.buildMessage(post, s);

        assertTrue(msg.contains("*Intro*"));
        assertTrue(msg.contains("*Stats*"));
    }

    @Test
    void shouldPublishBody_decisionMatrix() {
        User userOff = new User("U", "Off", "u@e");
        User userOn = new User("U", "On", "u@e");
        userOn.setSlackPublishPostBody(true);

        // master off → always false
        GroupSettings s1 = new GroupSettings();
        assertFalse(SlackNotificationServiceImpl.shouldPublishBody(s1, userOn));

        // master on, group override off, user off → false
        GroupSettings s2 = new GroupSettings();
        s2.setSlackPublishPostData(true);
        assertFalse(SlackNotificationServiceImpl.shouldPublishBody(s2, userOff));

        // master on, group override off, user on → true
        assertTrue(SlackNotificationServiceImpl.shouldPublishBody(s2, userOn));

        // master on, group override on, user off → true (group wins)
        GroupSettings s3 = new GroupSettings();
        s3.setSlackPublishPostData(true);
        s3.setSlackPublishPostBody(true);
        assertTrue(SlackNotificationServiceImpl.shouldPublishBody(s3, userOff));

        // null user → false unless group forces
        assertFalse(SlackNotificationServiceImpl.shouldPublishBody(s2, null));
        assertTrue(SlackNotificationServiceImpl.shouldPublishBody(s3, null));
    }

    @Test
    void shouldPublishStats_decisionMatrix() {
        User userOff = new User("U", "Off", "u@e");
        User userOn = new User("U", "On", "u@e");
        userOn.setSlackPublishStats(true);

        GroupSettings s1 = new GroupSettings();
        assertFalse(SlackNotificationServiceImpl.shouldPublishStats(s1, userOn));

        GroupSettings s2 = new GroupSettings();
        s2.setSlackPublishPostData(true);
        assertFalse(SlackNotificationServiceImpl.shouldPublishStats(s2, userOff));
        assertTrue(SlackNotificationServiceImpl.shouldPublishStats(s2, userOn));

        GroupSettings s3 = new GroupSettings();
        s3.setSlackPublishPostData(true);
        s3.setSlackPublishStats(true);
        assertTrue(SlackNotificationServiceImpl.shouldPublishStats(s3, userOff));
        assertTrue(SlackNotificationServiceImpl.shouldPublishStats(s3, null));
    }

    private Post createPost(String firstName, String lastName, Long id) {
        User user = new User(firstName, lastName, "test@example.com");
        Post post = new Post();
        post.setUser(user);
        post.setId(id);
        return post;
    }

    private Post createPostWithBodyAndStats(String firstName, String lastName, Long id) {
        Post post = createPost(firstName, lastName, id);

        Intro intro = new Intro();
        intro.setWidwytk("hidden thoughts");
        intro.setKryptonite("weak spot");
        intro.setWhatAndWhen("plan");
        post.setIntro(intro);

        Category personal = new Category();
        personal.setBest("ran");
        personal.setWorst("snoozed");
        post.setPersonal(personal);

        StatDefinition exercise = new StatDefinition("exercise", "Exercise", "💪", 0);
        exercise.setId(1L);
        List<PostStatValue> values = new ArrayList<>();
        values.add(new PostStatValue(post, exercise, 5));
        post.setStatValues(values);

        return post;
    }
}
