package com.afitnerd.tnra.slack.config;

import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlackSlashCommandConverterTests {

    private HttpInputMessage httpInputMessage;
    private SlackSlashCommandConverter slackSlashCommandConverter;

    @BeforeEach
    public void before() {

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        httpInputMessage = new HttpInputMessage() {

            String body = "token=blarg&team_id=T1DLRUNN8&team_domain=tnra&channel_id=D1DN187CH&" +
                "channel_name=directmessage&user_id=blarg&user_name=afitnerd&command=%2Fpost&text=help&" +
                "response_url=blarg&trigger_id=blarg";

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(body.getBytes("UTF-8"));
            }

            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };

        slackSlashCommandConverter = new SlackSlashCommandConverter();
    }

    @Test
    public void test_readInternal_success() throws IOException {
        SlackSlashCommandRequest request = slackSlashCommandConverter.readInternal(SlackSlashCommandRequest.class, httpInputMessage);
        assertEquals("help", request.getText());
    }

    @Test
    public void test_supports_true() {
        assertTrue(slackSlashCommandConverter.supports(SlackSlashCommandRequest.class));
    }

    @Test
    public void test_supports_false() {
        assertFalse(slackSlashCommandConverter.supports(String.class));
    }
}
