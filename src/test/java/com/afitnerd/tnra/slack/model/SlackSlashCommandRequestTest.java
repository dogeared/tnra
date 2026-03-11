package com.afitnerd.tnra.slack.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlackSlashCommandRequestTest {

    @Test
    void gettersToStringAndCommandStringExposeAllFields() {
        SlackSlashCommandRequest request = new SlackSlashCommandRequest();
        request.setToken("token");
        request.setCommand("/post");
        request.setText("show");
        request.setTeamId("team-id");
        request.setTeamDomain("team-domain");
        request.setChannelId("channel-id");
        request.setChannelName("channel-name");
        request.setUserId("user-id");
        request.setUserName("user-name");
        request.setResponseUrl("https://example.com/response");
        request.setTriggerId("trigger-id");

        assertEquals("token", request.getToken());
        assertEquals("/post", request.getCommand());
        assertEquals("show", request.getText());
        assertEquals("team-id", request.getTeamId());
        assertEquals("team-domain", request.getTeamDomain());
        assertEquals("channel-id", request.getChannelId());
        assertEquals("channel-name", request.getChannelName());
        assertEquals("user-id", request.getUserId());
        assertEquals("user-name", request.getUserName());
        assertEquals("https://example.com/response", request.getResponseUrl());
        assertEquals("trigger-id", request.getTriggerId());
        assertEquals("/post show\n", request.commandString());
        assertEquals(
            "token|/post|show|team-id|team-domain|channel-id|channel-name|user-id|user-name|https://example.com/response",
            request.toString()
        );
    }
}
