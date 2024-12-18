package com.afitnerd.tnra.slack;

import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.service.SlackSlashCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class SlackSlashCommandServiceTests {

    @Autowired
    SlackSlashCommandService slackSlashCommandService;

    SlackSlashCommandRequest request;

    @BeforeEach
    public void before() {
        request = new SlackSlashCommandRequest();
        request.setUserId("abc123-" + UUID.randomUUID());
        request.setUserName("afitnerd-"+ UUID.randomUUID());
    }

    @Test
    public void testToken_True() {
        request.setToken("my_special_token");
        assertTrue(slackSlashCommandService.isValidToken(request));
    }

    @Test
    public void testToken_False() {
        request.setToken("bad_token");
        assertFalse(slackSlashCommandService.isValidToken(request));
    }

    @Test
    public void testProcess_help() {
        request.setText("help");
        SlackSlashCommandResponse response = slackSlashCommandService.process(request);
        assertTrue(response.getText().contains("Here are the commands you can use to post"));
    }

    @Test
    public void testProcess_update_complete() {
        updateOrReplace("upd");
    }

    @Test
    public void testProcess_replace_complete() {
        updateOrReplace("rep");
    }

    @Test
    public void testProcesss_update_stat() {
        SlackSlashCommandResponse response = null;

        request.setText("sta");
        response = slackSlashCommandService.process(request);
        request.setText("upd stat exe:");
        response = slackSlashCommandService.process(request);
        assertTrue(response.getText().contains("*exercise:* 1"));
        response = slackSlashCommandService.process(request);
        assertTrue(response.getText().contains("*exercise:* 2"));
        response = slackSlashCommandService.process(request);
        assertTrue(response.getText().contains("*exercise:* 3"));
    }

    private void updateOrReplace(String updateOrReplace) {
        SlackSlashCommandResponse response = null;
        List<String> commands = Arrays.asList(
                "sta",
                updateOrReplace + " int wid wid",
                updateOrReplace + " int kry kry",
                updateOrReplace + " int wha wha",
                updateOrReplace + " per bes bes",
                updateOrReplace + " per wor wor",
                updateOrReplace + " fam bes bes",
                updateOrReplace + " fam wor wor",
                updateOrReplace + " wor bes bes",
                updateOrReplace + " wor wor wor",
                updateOrReplace + " sta exe:1 gtg:2 med:3 mee:4 pra:5 rea:6 spo:7",
                "fin"
        );
        for (String command : commands) {
            request.setText(command);
            response = slackSlashCommandService.process(request);
        }
        assertTrue(response.getText().contains("*exercise:* 1, *gtg:* 2, *meditate:* 3, *meetings:* 4, *pray:* 5, *read:* 6, *sponsor:* 7"));
        assertTrue(response.getText().contains("post started"));
        assertTrue(response.getText().contains("post finished"));
    }
}
