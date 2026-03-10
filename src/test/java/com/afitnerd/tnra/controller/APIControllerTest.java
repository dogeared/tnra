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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
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
    private GoToGuySetRepository gtgSetRepository;

    private APIController controller;

    @BeforeEach
    void setUp() {
        controller = new APIController(
            postService,
            eMailService,
            slackAPIService,
            slackPostRenderer,
            userRepository,
            gtgSetRepository
        );
    }

    @Test
    void notifyWhatAndWhensSkipsWhenNoGtgSetExists() {
        when(gtgSetRepository.findTopByOrderByStartDateDesc()).thenReturn(null);

        GoToGuySet result = controller.notifyWhatAndWhens();

        assertNull(result);
        verify(postService, never()).getLastFinishedPost(org.mockito.ArgumentMatchers.any(User.class));
        verify(eMailService, never()).sendTextViaMail(org.mockito.ArgumentMatchers.any(User.class), org.mockito.ArgumentMatchers.any(Post.class));
    }

    @Test
    void notifyWhatAndWhensSendsPartnerPostToEachRecipient() {
        User caller = new User();
        caller.setEmail("caller@example.com");
        User callee = new User();
        callee.setEmail("callee@example.com");

        Post callerPost = new Post();
        callerPost.setUser(caller);
        Post calleePost = new Post();
        calleePost.setUser(callee);

        GoToGuyPair pair = new GoToGuyPair();
        pair.setCaller(caller);
        pair.setCallee(callee);
        GoToGuySet set = new GoToGuySet();
        set.setGoToGuyPairs(List.of(pair));

        when(gtgSetRepository.findTopByOrderByStartDateDesc()).thenReturn(set);
        when(postService.getLastFinishedPost(callee)).thenReturn(calleePost);
        when(postService.getLastFinishedPost(caller)).thenReturn(callerPost);

        GoToGuySet result = controller.notifyWhatAndWhens();

        assertSame(set, result);
        verify(eMailService).sendTextViaMail(caller, calleePost);
        verify(eMailService).sendTextViaMail(callee, callerPost);
    }
}
