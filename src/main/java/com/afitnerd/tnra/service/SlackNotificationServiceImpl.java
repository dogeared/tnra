package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SlackNotificationServiceImpl implements SlackNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationServiceImpl.class);

    private final GroupSettingsService groupSettingsService;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackNotificationServiceImpl(
        GroupSettingsService groupSettingsService,
        @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.groupSettingsService = groupSettingsService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    @Async("slackTaskExecutor")
    public void sendActivityNotification(Post post) {
        GroupSettings settings = groupSettingsService.getSettings();
        if (!settings.isSlackEnabled()) {
            log.debug("Slack notifications disabled — skipping");
            return;
        }
        String webhookUrl = settings.getSlackWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("No Slack webhook URL configured — skipping");
            return;
        }

        String message = buildMessage(post);
        try {
            String payload = objectMapper.writeValueAsString(Map.of("text", message));
            doPost(webhookUrl, payload);
            log.info("Slack activity notification sent for post id={}", post.getId());
        } catch (IOException e) {
            log.error("Failed to send Slack activity notification for post id={}: {}", post.getId(), e.getMessage(), e);
        }
    }

    void doPost(String webhookUrl, String payload) throws IOException {
        Request.post(webhookUrl)
            .bodyString(payload, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8))
            .execute()
            .discardContent();
    }

    String buildMessage(Post post) {
        User user = post.getUser();
        String username = "Someone";
        if (user != null) {
            String first = user.getFirstName();
            String last = user.getLastName();
            if (first != null && !first.isBlank() && last != null && !last.isBlank()) {
                username = first + " " + last;
            } else if (first != null && !first.isBlank()) {
                username = first;
            } else if (user.getEmail() != null) {
                username = user.getEmail();
            }
        }

        String started = post.getStart() != null ? PostRenderer.formatDate(post.getStart()) : "unknown";
        String finished = post.getFinish() != null ? PostRenderer.formatDate(post.getFinish()) : "unknown";
        String postUrl = baseUrl + "/posts/" + (post.getId() != null ? post.getId() : "");

        return username + " finished a post | Started: " + started + " | Finished: " + finished + " | View: " + postUrl;
    }
}
