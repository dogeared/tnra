package com.afitnerd.tnra.slack.controller;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.service.SlackSlashCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
public class SlackController {

    private SlackSlashCommandService slackSlashCommandService;

    private static final String POST_COMMAND = "/post";

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SlackController.class);

    private void echoCommand(SlackSlashCommandRequest slackSlashCommandRequest) {
        String command = slackSlashCommandRequest.commandString();
        try {
            SlackSlashCommandResponse slackSlashCommandResponse = new SlackSlashCommandResponse();
            slackSlashCommandResponse.setText(command);
            StringEntity body = new StringEntity(mapper.writeValueAsString(slackSlashCommandResponse));

            Request.Post(slackSlashCommandRequest.getResponseUrl())
                .body(body)
                .execute();
        } catch (IOException e) {
            log.error("Couldn't POST to: {} with {}", slackSlashCommandRequest.getResponseUrl(), command, e);
        }
    }

    public SlackController(SlackSlashCommandService slackSlashCommandService) {
        this.slackSlashCommandService = slackSlashCommandService;
    }

    @RequestMapping(
        value = POST_COMMAND, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse post(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        log.debug("slackSlashCommandRequest: {}", slackSlashCommandRequest);

        if (!slackSlashCommandService.isValidToken(slackSlashCommandRequest)) {
            throw new IllegalArgumentException("Slack token is incorrect.");
        }

        echoCommand(slackSlashCommandRequest);

        return slackSlashCommandService.process(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = "/tnra", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse help(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("help");
        return post(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = {"/show", "/start", "/finish"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sho(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String command = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText(command + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = {"/wid", "/kry", "/wha"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse wid(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String introSection = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd int " + introSection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = {"/per", "/fam", "/wor"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse per(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String categorySection = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd " + categorySection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = "/sta", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sta(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd sta " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = {"/exe", "/gtg", "/med", "/mee", "/pra", "/rea", "/spo"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse staSpecific(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String stat = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd sta " + stat + ":" + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @ExceptionHandler({IllegalArgumentException.class, PostException.class})
    public SlackSlashCommandResponse handleException(Exception e, HttpServletRequest httpServletRequest) {
        String msg = e.getMessage();
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        response.setText(msg);
        return response;
    }
}
