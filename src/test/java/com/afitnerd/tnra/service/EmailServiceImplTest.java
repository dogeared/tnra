package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    private PostRenderer postRenderer;
    private UserRepository userRepository;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        postRenderer = mock(PostRenderer.class);
        userRepository = mock(UserRepository.class);
        emailService = new EmailServiceImpl(postRenderer, userRepository);
        ReflectionTestUtils.setField(emailService, "mailgunPrivateKey", "test-key");
        ReflectionTestUtils.setField(emailService, "mailgunPublicKey", "test-pub");
        ReflectionTestUtils.setField(emailService, "mailgunUrl", "https://api.mailgun.net/v3/test/messages");
    }

    @Test
    void sendMailToMe_skipsWhenDisabled() {
        ReflectionTestUtils.setField(emailService, "emailServiceEnabled", false);

        User user = new User("Test", "User", "test@example.com");
        Post post = new Post();
        post.setUser(user);

        emailService.sendMailToMe(user, post);

        verifyNoInteractions(postRenderer);
    }

    @Test
    void sendMailToAll_skipsWhenDisabled() {
        ReflectionTestUtils.setField(emailService, "emailServiceEnabled", false);

        Post post = new Post();
        post.setUser(new User("Author", "User", "author@example.com"));

        emailService.sendMailToAll(post);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(postRenderer);
    }

    @Test
    void sendMailToAll_skipsUsersWithNotificationsDisabled() {
        ReflectionTestUtils.setField(emailService, "emailServiceEnabled", true);

        User noNotify = new User("No", "Notify", "no@example.com");
        noNotify.setNotifyNewPosts(false);

        // Only user with notifications explicitly disabled
        when(userRepository.findByActiveTrue()).thenReturn(List.of(noNotify));

        Post post = new Post();
        post.setUser(new User("Author", "User", "author@example.com"));

        emailService.sendMailToAll(post);

        verify(userRepository).findByActiveTrue();
        verifyNoInteractions(postRenderer);
    }

    @Test
    void sendMailToAll_callsRenderForUsersWithNotificationsEnabled() {
        ReflectionTestUtils.setField(emailService, "emailServiceEnabled", true);

        User wantsNotify = new User("Yes", "Notify", "yes@example.com");
        wantsNotify.setNotifyNewPosts(true);

        User noNotify = new User("No", "Notify", "no@example.com");
        noNotify.setNotifyNewPosts(false);

        when(userRepository.findByActiveTrue()).thenReturn(List.of(wantsNotify, noNotify));
        when(postRenderer.render(any())).thenReturn("<p>activity</p>");

        Post post = new Post();
        post.setUser(new User("Author", "User", "author@example.com"));

        // sendMailToMe will attempt the HTTP call and fail (no real Mailgun),
        // but the render call proves the right user was selected
        emailService.sendMailToAll(post);

        // render is called once for wantsNotify, not for noNotify
        verify(postRenderer, times(1)).render(post);
    }
}
