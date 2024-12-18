package com.afitnerd.tnra.slack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.client5.http.fluent.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class SlackAPIServiceImpl implements SlackAPIService {

    @Value("#{ @environment['tnra.slack.access_token'] ?: '' }")
    private String slackAccessToken;

    @Value("#{ @environment['tnra.slack.broadcast_channel'] ?: 'general' }")
    private String broadcastChannel;

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public Map<String, Object> chat(String text) {

        Map<String, Object> request = Map.of(
            "channel", "#" + broadcastChannel,
            "text", text
        );

        try {
            InputStream is = Request.post(SLACK_BASE_URL + SLACK_CHAT_POST_MESSAGE)
                .addHeader("Authorization", "Bearer " + slackAccessToken)
                .bodyString(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON)
                .execute()
                .returnContent()
                .asStream();
            return mapper.readValue(is, Map.class);
        } catch (IOException e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
