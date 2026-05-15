package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
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
    private static final String SLACK_WEBHOOK_URL_PREFIX = "https://hooks.slack.com/";

    private final GroupSettingsService groupSettingsService;
    private final PostTokenService postTokenService;
    private final SlackPostBodyRenderer slackPostBodyRenderer;
    private final SlackStatsRenderer slackStatsRenderer;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SlackNotificationServiceImpl(
        GroupSettingsService groupSettingsService,
        PostTokenService postTokenService,
        SlackPostBodyRenderer slackPostBodyRenderer,
        SlackStatsRenderer slackStatsRenderer,
        @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.groupSettingsService = groupSettingsService;
        this.postTokenService = postTokenService;
        this.slackPostBodyRenderer = slackPostBodyRenderer;
        this.slackStatsRenderer = slackStatsRenderer;
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

        String message = buildMessage(post, settings);
        try {
            String payload = objectMapper.writeValueAsString(Map.of("text", message));
            doPost(webhookUrl, payload);
            log.info("Slack activity notification sent for post token={}", post.getId() != null ? postTokenService.encode(post.getId()) : "null");
        } catch (Exception e) {
            log.error("Failed to send Slack activity notification for post token={}: {}", post.getId() != null ? postTokenService.encode(post.getId()) : "null", e.getMessage(), e);
        }
    }

    void doPost(String webhookUrl, String payload) throws IOException {
        if (!webhookUrl.startsWith(SLACK_WEBHOOK_URL_PREFIX)) {
            throw new IllegalArgumentException("Webhook URL must be a Slack incoming webhook URL");
        }
        Request.post(webhookUrl)
            .connectTimeout(Timeout.ofSeconds(3))
            .responseTimeout(Timeout.ofSeconds(5))
            .bodyString(payload, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8))
            .execute()
            .discardContent();
    }

    String buildMessage(Post post, GroupSettings settings) {
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
        String postUrl = (post.getId() != null)
            ? baseUrl + "/posts/" + postTokenService.encode(post.getId())
            : baseUrl + "/posts/";

        StringBuilder message = new StringBuilder();
        message.append(escapeSlack(username))
            .append(" finished a post | Started: ").append(started)
            .append(" | Finished: ").append(finished)
            .append(" | View <").append(postUrl).append("|here>");

        // Body before stats when both are requested.
        if (shouldPublishBody(settings, user)) {
            String body = slackPostBodyRenderer.render(post);
            if (body != null && !body.isBlank()) {
                message.append("\n\n").append(body);
            }
        }
        if (shouldPublishStats(settings, user)) {
            String stats = slackStatsRenderer.render(post);
            if (stats != null && !stats.isBlank()) {
                message.append("\n\n").append(stats);
            }
        }
        return message.toString();
    }

    static boolean shouldPublishBody(GroupSettings settings, User user) {
        if (!settings.isSlackPublishPostData()) {
            return false;
        }
        return settings.isSlackPublishPostBody()
            || (user != null && Boolean.TRUE.equals(user.getSlackPublishPostBody()));
    }

    static boolean shouldPublishStats(GroupSettings settings, User user) {
        if (!settings.isSlackPublishPostData()) {
            return false;
        }
        return settings.isSlackPublishStats()
            || (user != null && Boolean.TRUE.equals(user.getSlackPublishStats()));
    }

    private static String escapeSlack(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
