package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VaadinPostServiceImpl implements VaadinPostService {

    private final PostService postService;
    private final EMailService eMailService;
    private final SlackAPIService slackAPIService;
    private final SlackPostRenderer slackPostRenderer;

    @Value("#{ @environment['tnra.emailService.enabled'] ?: true }")
    private boolean emailServiceEnabled;

    @Value("#{ @environment['tnra.slackService.enabled'] ?: true }")
    private boolean slackServiceEnabled;

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
        if (emailServiceEnabled) {
            eMailService.sendMailToAll(post);
        }
        if (slackServiceEnabled) {
            // use chat api to send to general
            Map<String, Object> charRes = slackAPIService.chat(slackPostRenderer.render(post));
        }

        return post;
    }
}
