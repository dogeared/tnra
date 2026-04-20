package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PersonalStatDefinitionRepository;
import com.afitnerd.tnra.repository.PostRepository;
import com.afitnerd.tnra.repository.StatDefinitionRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaadinPostPresenterImplTest {

    private OidcUserService oidcUserService;
    private UserService userService;
    private PostService postService;
    private EMailService emailService;
    private PostRepository postRepository;
    private StatDefinitionRepository statDefinitionRepository;
    private PersonalStatDefinitionRepository personalStatDefinitionRepository;
    private VaadinPostPresenterImpl presenter;

    @BeforeEach
    void setUp() {
        oidcUserService = mock(OidcUserService.class);
        userService = mock(UserService.class);
        postService = mock(PostService.class);
        emailService = mock(EMailService.class);
        postRepository = mock(PostRepository.class);
        statDefinitionRepository = mock(StatDefinitionRepository.class);
        personalStatDefinitionRepository = mock(PersonalStatDefinitionRepository.class);
        presenter = new VaadinPostPresenterImpl(
            oidcUserService, userService, postService, emailService, postRepository, statDefinitionRepository, personalStatDefinitionRepository
        );
    }

    @Test
    void finishPostRespectsEmailServiceFlag() throws Exception {
        User user = new User();
        Post post = new Post();
        when(postService.finishPost(user)).thenReturn(post);

        setField(presenter, "emailServiceEnabled", true);

        assertSame(post, presenter.finishPost(user));
        verify(emailService).sendMailToAll(post);

        setField(presenter, "emailServiceEnabled", false);
        presenter.finishPost(user);
        verify(emailService, org.mockito.Mockito.times(1)).sendMailToAll(post);
    }

    @Test
    void initializeUserValidatesAuthenticationAndPresence() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> presenter.initializeUser());

        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> presenter.initializeUser());

        User user = new User();
        user.setActive(true);
        when(userService.getUserByEmail("user@example.com")).thenReturn(user);
        assertSame(user, presenter.initializeUser());
    }

    @Test
    void initializeUserThrowsForDeactivatedUser() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("deactivated@example.com");
        User deactivated = new User();
        deactivated.setActive(false);
        when(userService.getUserByEmail("deactivated@example.com")).thenReturn(deactivated);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> presenter.initializeUser());
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    void delegatesRemainingPostOperations() {
        User user = new User();
        user.setActive(true);
        Post post = new Post();
        StatDefinition statDef = new StatDefinition("exercise", "Exercise", "💪", 0);

        when(postService.getOptionalInProgressPost(user)).thenReturn(Optional.of(post));
        when(postService.getCompletedPostsPage(eq(user), any())).thenReturn(new PageImpl<>(List.of(post)));
        when(postService.startPost(user)).thenReturn(post);
        when(postService.savePost(post)).thenReturn(post);
        when(userService.getAllActiveUsers()).thenReturn(List.of(user));

        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(user);
        when(postService.updateStatValue(user, statDef, 5)).thenReturn(post);

        assertEquals(Optional.of(post), presenter.getOptionalInProgressPost(user));
        assertEquals(1, presenter.getCompletedPostsPage(user, PageRequest.of(0, 5)).getTotalElements());
        assertSame(post, presenter.startPost(user));
        assertSame(post, presenter.savePost(post));
        assertSame(post, presenter.updateStatValue(statDef, 5));
        assertEquals(1, presenter.getAllActiveUsers().size());
    }

    @Test
    void getPostByIdDelegatesToRepository() {
        Post post = new Post();
        when(postRepository.findById(42L)).thenReturn(Optional.of(post));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals(Optional.of(post), presenter.getPostById(42L));
        assertEquals(Optional.empty(), presenter.getPostById(999L));
        verify(postRepository).findById(42L);
        verify(postRepository).findById(999L);
    }

    @Test
    void getActiveStatDefinitionsDelegatesToRepository() {
        List<StatDefinition> defs = List.of(new StatDefinition("exercise", "Exercise", "💪", 0));
        when(statDefinitionRepository.findGlobalActiveOrderByDisplayOrderAsc()).thenReturn(defs);

        assertEquals(1, presenter.getActiveGlobalStatDefinitions().size());
        verify(statDefinitionRepository).findGlobalActiveOrderByDisplayOrderAsc();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
