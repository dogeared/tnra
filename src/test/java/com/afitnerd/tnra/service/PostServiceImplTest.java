package com.afitnerd.tnra.service;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.*;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceImplTest {

    private PostRepository postRepository;
    private StatDefinitionRepository statDefinitionRepository;
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private PostServiceImpl postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        statDefinitionRepository = mock(StatDefinitionRepository.class);
        personalStatDefinitionRepository = mock(PersonalStatDefinitionRepository.class);
        postService = new PostServiceImpl(postRepository, statDefinitionRepository, personalStatDefinitionRepository);
    }

    // --- getUserDisplayName edge cases (exercised through error messages) ---

    @Test
    void errorMessageUsesFirstAndLastName() {
        User user = new User("John", "Doe", "john@example.com");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        assertTrue(ex.getMessage().contains("John Doe"));
    }

    @Test
    void errorMessageUsesEmailWhenNameMissing() {
        User user = new User(null, null, "anon@example.com");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        assertTrue(ex.getMessage().contains("anon@example.com"));
    }

    @Test
    void errorMessageUsesEmailWhenNameIsBlank() {
        User user = new User("  ", "  ", "blank@example.com");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        assertTrue(ex.getMessage().contains("blank@example.com"));
    }

    @Test
    void errorMessageUsesUnknownUserWhenAllNull() {
        User user = new User(null, null, null);
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        assertTrue(ex.getMessage().contains("unknown user"));
    }

    @Test
    void errorMessageUsesUnknownUserWhenAllBlank() {
        User user = new User("", "", "");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        assertTrue(ex.getMessage().contains("unknown user"));
    }

    @Test
    void errorMessageUsesEmailWhenOnlyFirstNamePresent() {
        User user = new User("John", null, "john@example.com");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.getInProgressPost(user));
        // firstName alone isn't enough — should fall back to email
        assertTrue(ex.getMessage().contains("john@example.com"));
    }

    // --- startPost ---

    @Test
    void startPost_throwsOnNullUser() {
        assertThrows(IllegalArgumentException.class, () -> postService.startPost(null));
    }

    @Test
    void startPost_createsPostWhenNoneInProgress() {
        User user = new User("Test", "User", "test@example.com");
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(Collections.emptyList());
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post post = postService.startPost(user);

        assertNotNull(post);
        assertEquals(user, post.getUser());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void startPost_throwsWhenAlreadyInProgress() {
        User user = new User("Test", "User", "test@example.com");
        Post existing = new Post(user);
        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(List.of(existing));

        PostException ex = assertThrows(PostException.class, () -> postService.startPost(user));
        assertTrue(ex.getMessage().contains("already in progress"));
    }

    // --- finishPost with grace period ---

    @Test
    void finishPost_skipsStatsCreatedAfterPostStarted() {
        User user = new User("Test", "User", "test@example.com");
        Date postStart = new Date(System.currentTimeMillis() - 60_000); // 1 minute ago

        Post post = new Post(user);
        // Set all required fields
        post.getIntro().setWidwytk("test");
        post.getIntro().setKryptonite("test");
        post.getIntro().setWhatAndWhen("test");
        post.getPersonal().setBest("test");
        post.getPersonal().setWorst("test");
        post.getFamily().setBest("test");
        post.getFamily().setWorst("test");
        post.getWork().setBest("test");
        post.getWork().setWorst("test");

        // Simulate post start time
        post.setStart(postStart);

        // Global stat created BEFORE the post — required
        StatDefinition oldStat = new StatDefinition("exercise", "Exercise", "💪", 0);
        oldStat.setId(1L);
        oldStat.setCreatedAt(new Date(postStart.getTime() - 120_000)); // 2 min before post
        post.setStatValue(oldStat, 5);

        // Global stat created AFTER the post — should be skipped (grace period)
        StatDefinition newStat = new StatDefinition("newstat", "New Stat", "🆕", 1);
        newStat.setId(2L);
        newStat.setCreatedAt(new Date(postStart.getTime() + 30_000)); // 30s after post started

        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(List.of(post));
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(List.of(oldStat, newStat));
        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(user))
            .thenReturn(Collections.emptyList());
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post finished = postService.finishPost(user);

        assertEquals(PostState.COMPLETE, finished.getState());
        assertNotNull(finished.getFinish());
    }

    @Test
    void finishPost_requiresStatsCreatedBeforePostStarted() {
        User user = new User("Test", "User", "test@example.com");
        Date postStart = new Date(System.currentTimeMillis() - 60_000);

        Post post = new Post(user);
        post.getIntro().setWidwytk("test");
        post.getIntro().setKryptonite("test");
        post.getIntro().setWhatAndWhen("test");
        post.getPersonal().setBest("test");
        post.getPersonal().setWorst("test");
        post.getFamily().setBest("test");
        post.getFamily().setWorst("test");
        post.getWork().setBest("test");
        post.getWork().setWorst("test");
        post.setStart(postStart);

        // Stat created before post but NOT filled in — should fail
        StatDefinition requiredStat = new StatDefinition("exercise", "Exercise", "💪", 0);
        requiredStat.setCreatedAt(new Date(postStart.getTime() - 120_000));

        when(postRepository.findByUserAndState(user, PostState.IN_PROGRESS))
            .thenReturn(List.of(post));
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc())
            .thenReturn(List.of(requiredStat));
        when(personalStatDefinitionRepository.findByUserAndArchivedFalseOrderByDisplayOrderAsc(user))
            .thenReturn(Collections.emptyList());

        PostException ex = assertThrows(PostException.class, () -> postService.finishPost(user));
        assertTrue(ex.getMessage().contains("exercise"));
    }

    // --- getOptionalInProgressPost / getOptionalCompletePost ---

    @Test
    void getOptionalInProgressPost_throwsOnNullUser() {
        assertThrows(IllegalArgumentException.class, () -> postService.getOptionalInProgressPost(null));
    }

    @Test
    void getOptionalCompletePost_throwsOnNullUser() {
        assertThrows(IllegalArgumentException.class, () -> postService.getOptionalCompletePost(null));
    }

    @Test
    void getOptionalInProgressPost_returnsEmptyWhenNone() {
        User user = new User("Test", "User", "test@example.com");
        when(postRepository.findFirstByUserAndStateOrderByFinishDesc(user, PostState.IN_PROGRESS))
            .thenReturn(Optional.empty());

        assertTrue(postService.getOptionalInProgressPost(user).isEmpty());
    }

    // --- savePost ---

    @Test
    void savePost_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> postService.savePost(null));
    }

    // --- getLastFinishedPost ---

    @Test
    void getLastFinishedPost_throwsWhenNoFinishedPosts() {
        User user = new User("Test", "User", "test@example.com");
        when(postRepository.findFirstByUserAndStateOrderByFinishDesc(user, PostState.COMPLETE))
            .thenReturn(Optional.empty());

        PostException ex = assertThrows(PostException.class, () -> postService.getLastFinishedPost(user));
        assertTrue(ex.getMessage().contains("no finished posts"));
    }
}
