package com.afitnerd.tnra.slack.controller;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.service.pq.PQService;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import com.afitnerd.tnra.slack.service.SlackSlashCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackControllerTest {

    @Mock
    private SlackSlashCommandService slackSlashCommandService;
    @Mock
    private SlackAPIService slackAPIService;
    @Mock
    private PQService pqService;

    private SlackController controller;

    @BeforeEach
    void setUp() {
        controller = new SlackController(slackSlashCommandService, slackAPIService, pqService);
        lenient().when(slackSlashCommandService.isValidToken(any(SlackSlashCommandRequest.class))).thenReturn(false);
    }

    @Test
    void postRejectsInvalidSlackToken() {
        SlackSlashCommandRequest request = request("/post", "show");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> controller.post(request));

        assertEquals("Slack token is incorrect.", exception.getMessage());
    }

    @Test
    void pqRejectsInvalidSlackTokenBeforeAsyncWork() {
        SlackSlashCommandRequest request = request("/pq", "");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> controller.pq(request));

        assertEquals("Slack token is incorrect.", exception.getMessage());
    }

    @Test
    void helpRewritesToPostHelpCommand() {
        SlackSlashCommandRequest request = request("/tnra", "ignored");

        assertThrows(IllegalArgumentException.class, () -> controller.help(request));

        assertEquals("/post", request.getCommand());
        assertEquals("help", request.getText());
    }

    @Test
    void shortcutCommandPrefixesExistingText() {
        SlackSlashCommandRequest request = request("/show", "status");

        assertThrows(IllegalArgumentException.class, () -> controller.sho(request));

        assertEquals("/post", request.getCommand());
        assertEquals("show status", request.getText());
    }

    @Test
    void introShortcutRewritesToIntroUpdateCommand() {
        SlackSlashCommandRequest request = request("/wid", "focus");

        assertThrows(IllegalArgumentException.class, () -> controller.wid(request));

        assertEquals("/post", request.getCommand());
        assertEquals("upd int wid focus", request.getText());
    }

    @Test
    void categoryShortcutRewritesToCategoryUpdateCommand() {
        SlackSlashCommandRequest request = request("/per", "best day");

        assertThrows(IllegalArgumentException.class, () -> controller.per(request));

        assertEquals("/post", request.getCommand());
        assertEquals("upd per best day", request.getText());
    }

    @Test
    void staShortcutRewritesToStatsUpdateCommand() {
        SlackSlashCommandRequest request = request("/sta", "exe:3");

        assertThrows(IllegalArgumentException.class, () -> controller.sta(request));

        assertEquals("/post", request.getCommand());
        assertEquals("upd sta exe:3", request.getText());
    }

    @Test
    void staSpecificShortcutRewritesToNamedStatUpdateCommand() {
        SlackSlashCommandRequest request = request("/med", "2");

        assertThrows(IllegalArgumentException.class, () -> controller.staSpecific(request));

        assertEquals("/post", request.getCommand());
        assertEquals("upd sta med:2", request.getText());
    }

    @Test
    void exceptionHandlerReturnsMessageAsSlackResponse() {
        SlackSlashCommandResponse response = controller.handleException(
            new PostException("bad command"),
            null
        );

        assertEquals("bad command", response.getText());
    }

    private static SlackSlashCommandRequest request(String command, String text) {
        SlackSlashCommandRequest request = new SlackSlashCommandRequest();
        request.setCommand(command);
        request.setText(text);
        request.setToken("bad-token");
        return request;
    }
}
