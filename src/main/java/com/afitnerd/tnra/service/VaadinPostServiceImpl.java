package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.slack.service.SlackAPIService;

import java.util.Map;

public class VaadinPostServiceImpl implements VaadinPostService {

    private final PostService postService;
    private final EMailService eMailService;
    private final SlackAPIService slackAPIService;
    private final SlackPostRenderer slackPostRenderer;

    public VaadinPostServiceImpl(
        PostService postService, EMailService eMailService,
        SlackAPIService slackAPIService, SlackPostRenderer slackPostRenderer
    ) {
        this.postService = postService;
        this.eMailService = eMailService;
        this.slackAPIService = slackAPIService;
        this.slackPostRenderer = slackPostRenderer;
    }

    @Override
    public Post finishPost(User user) {
        Post post = postService.finishPost(user);
        eMailService.sendMailToAll(post);
        // use chat api to send to general
        Map<String, Object> charRes = slackAPIService.chat(slackPostRenderer.render(post));

        return post;
    }
}
