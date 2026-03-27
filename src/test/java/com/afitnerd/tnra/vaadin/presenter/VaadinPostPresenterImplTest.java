package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaadinPostPresenterImplTest {

    private OidcUserService oidcUserService;
    private UserService userService;
    private PostService postService;
    private EMailService emailService;
    private VaadinPostPresenterImpl presenter;

    @BeforeEach
    void setUp() {
        oidcUserService = mock(OidcUserService.class);
        userService = mock(UserService.class);
        postService = mock(PostService.class);
        emailService = mock(EMailService.class);
        presenter = new VaadinPostPresenterImpl(
            oidcUserService, userService, postService, emailService
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
        when(userService.getUserByEmail("user@example.com")).thenReturn(user);
        assertSame(user, presenter.initializeUser());
    }

    @Test
    void delegatesRemainingPostOperations() {
        User user = new User();
        Post post = new Post();
        Stats stats = new Stats();

        when(postService.getOptionalInProgressPost(user)).thenReturn(Optional.of(post));
        when(postService.getCompletedPostsPage(eq(user), any())).thenReturn(new PageImpl<>(List.of(post)));
        when(postService.startPost(user)).thenReturn(post);
        when(postService.savePost(post)).thenReturn(post);
        when(userService.getAllActiveUsers()).thenReturn(List.of(user));

        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(user);
        when(postService.updateCompleteStats(user, stats)).thenReturn(post);

        assertEquals(Optional.of(post), presenter.getOptionalInProgressPost(user));
        assertEquals(1, presenter.getCompletedPostsPage(user, PageRequest.of(0, 5)).getTotalElements());
        assertSame(post, presenter.startPost(user));
        assertSame(post, presenter.savePost(post));
        assertSame(post, presenter.updateCompleteStats(stats));
        assertEquals(1, presenter.getAllActiveUsers().size());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(postService).updateCompleteStats(userCaptor.capture(), eq(stats));
        assertSame(user, userCaptor.getValue());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
