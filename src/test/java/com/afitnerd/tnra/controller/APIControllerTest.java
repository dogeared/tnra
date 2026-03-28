package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class APIControllerTest {

    @Mock
    private PostService postService;
    @Mock
    private EMailService eMailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GoToGuySetRepository goToGuySetRepository;

    @Test
    void returnsPrincipalUsersAndLatestGtgSet() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        List<User> users = List.of(new User("A", "User", "a@example.com"));
        GoToGuySet gtgSet = new GoToGuySet();

        when(userRepository.findByActiveTrue()).thenReturn(users);
        when(goToGuySetRepository.findTopByOrderByStartDateDesc()).thenReturn(gtgSet);

        assertSame(principal, controller.me(principal));
        assertSame(users, controller.users());
        assertSame(gtgSet, controller.gtgLatest());
    }

    @Test
    void inProgressCompleteStartAndUpdateUseCurrentUser() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        User user = new User("Test", "User", "user@example.com");
        Post existingPost = new Post(user);
        Post inputPost = new Post(user);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(postService.getOptionalInProgressPost(user)).thenReturn(Optional.of(existingPost));
        when(postService.getOptionalCompletePost(user)).thenReturn(Optional.of(existingPost));
        when(postService.startPost(user)).thenReturn(existingPost);
        when(postService.savePost(existingPost)).thenReturn(existingPost);

        assertEquals(Optional.of(existingPost), controller.getInProgressPost(principal));
        assertSame(existingPost, controller.updatePost(principal, inputPost));
        assertEquals(Optional.of(existingPost), controller.getCompletePost(principal));
        assertSame(existingPost, controller.startPost(principal));
    }

    @Test
    void updatePostThrowsWhenNoInProgressPost() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        User user = new User("Test", "User", "user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(postService.getOptionalInProgressPost(user)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.updatePost(principal, new Post(user)));
    }

    @Test
    void finishPostSendsEmail() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        User user = new User("Test", "User", "user@example.com");
        Post post = new Post(user);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(postService.finishPost(user)).thenReturn(post);

        Post finished = controller.finishPost(principal);

        assertSame(post, finished);
        verify(eMailService).sendMailToAll(post);
    }

    private APIController controller() {
        return new APIController(
            postService,
            eMailService,
            userRepository,
            goToGuySetRepository
        );
    }
}
