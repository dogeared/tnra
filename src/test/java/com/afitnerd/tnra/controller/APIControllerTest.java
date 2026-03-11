package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.GoToGuyPair;
import com.afitnerd.tnra.model.GoToGuySet;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.GoToGuySetRepository;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.EMailService;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.service.SlackPostRenderer;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class APIControllerTest {

    @Mock
    private PostService postService;
    @Mock
    private EMailService eMailService;
    @Mock
    private SlackAPIService slackAPIService;
    @Mock
    private SlackPostRenderer slackPostRenderer;
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
    void notifyWhatAndWhensSendsLastFinishedPostsForEachPair() {
        APIController controller = controller();
        User caller = new User("Caller", "One", "caller@example.com");
        User callee = new User("Callee", "Two", "callee@example.com");
        GoToGuyPair pair = new GoToGuyPair();
        pair.setCaller(caller);
        pair.setCallee(callee);
        GoToGuySet gtgSet = new GoToGuySet();
        gtgSet.setGoToGuyPairs(List.of(pair));
        Post callerPost = new Post(caller);
        Post calleePost = new Post(callee);
        when(goToGuySetRepository.findTopByOrderByStartDateDesc()).thenReturn(gtgSet);
        when(postService.getLastFinishedPost(callee)).thenReturn(calleePost);
        when(postService.getLastFinishedPost(caller)).thenReturn(callerPost);

        GoToGuySet returned = controller.notifyWhatAndWhens();

        assertSame(gtgSet, returned);
        verify(eMailService).sendTextViaMail(callee, calleePost);
        verify(eMailService).sendTextViaMail(callee, callerPost);
    }

    @Test
    void inProgressCompleteStartAndUpdateUseCurrentUser() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        User user = new User("Test", "User", "user@example.com");
        Post post = new Post(user);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(postService.getOptionalInProgressPost(user)).thenReturn(Optional.of(post));
        when(postService.getOptionalCompletePost(user)).thenReturn(Optional.of(post));
        when(postService.startPost(user)).thenReturn(post);
        when(postService.savePost(post)).thenReturn(post);

        assertEquals(Optional.of(post), controller.getInProgressPost(principal));
        assertSame(post, controller.updatePost(principal, post));
        assertEquals(Optional.of(post), controller.getCompletePost(principal));
        assertSame(post, controller.startPost(principal));
    }

    @Test
    void finishPostSendsEmailAndSlackMessage() {
        APIController controller = controller();
        Principal principal = () -> "user@example.com";
        User user = new User("Test", "User", "user@example.com");
        Post post = new Post(user);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);
        when(postService.finishPost(user)).thenReturn(post);
        when(slackPostRenderer.render(post)).thenReturn("finished");

        Post finished = controller.finishPost(principal);

        assertSame(post, finished);
        verify(eMailService).sendMailToAll(post);
        verify(slackAPIService).chat(any(String.class));
    }

    private APIController controller() {
        return new APIController(
            postService,
            eMailService,
            slackAPIService,
            slackPostRenderer,
            userRepository,
            goToGuySetRepository
        );
    }
}
