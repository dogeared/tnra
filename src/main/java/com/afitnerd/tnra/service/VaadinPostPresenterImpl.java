package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class VaadinPostPresenterImpl implements VaadinPostPresenter {

    private final UserService userService;
    private final OidcUserService oidcUserService;
    private final PostService postService;
    private final EMailService eMailService;
    private final SlackAPIService slackAPIService;
    private final SlackPostRenderer slackPostRenderer;

    @Value("#{ @environment['tnra.emailService.enabled'] ?: true }")
    private boolean emailServiceEnabled;

    @Value("#{ @environment['tnra.slackService.enabled'] ?: true }")
    private boolean slackServiceEnabled;

    public VaadinPostPresenterImpl(
        OidcUserService oidcUserService, UserService userService,
        PostService postService, EMailService eMailService,
        SlackAPIService slackAPIService, SlackPostRenderer slackPostRenderer
    ) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        this.postService = postService;
        this.eMailService = eMailService;
        this.slackAPIService = slackAPIService;
        this.slackPostRenderer = slackPostRenderer;
    }

    @Override
    public Post finishPost(User user) {
        Post post = postService.finishPost(user);
        if (emailServiceEnabled) {
            eMailService.sendMailToAll(post);
        }
        if (slackServiceEnabled) {
            // use chat api to send to general
            Map<String, Object> charRes = slackAPIService.chat(slackPostRenderer.render(post));
        }

        return post;
    }

    @Override
    public Optional<Post> getOptionalInProgressPost(User me) {
        return postService.getOptionalInProgressPost(me);
    }

    @Override
    public User initializeUser() {
        if (oidcUserService.isAuthenticated()) {
            String email = oidcUserService.getEmail();
            User currentUser = userService.getUserByEmail(email);

            if (currentUser == null) {
                throw new IllegalStateException("No user found with email: " + email);
            }

            return currentUser;
        } else {
            throw new IllegalStateException("User is not authenticated");
        }
    }

    @Override
    public Page<Post> getCompletedPostsPage(User me, Pageable pageable) {
        return postService.getCompletedPostsPage(me, pageable);
    }

    @Override
    public Post startPost(User me) {
        return postService.startPost(me);
    }

    @Override
    public Post savePost(Post post) {
        return postService.savePost(post);
    }

    @Override
    public Post updateCompleteStats(Stats stats) {
        return postService.updateCompleteStats(initializeUser(), stats);
    }
}
