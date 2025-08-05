package com.afitnerd.tnra.slack.controller;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.afitnerd.tnra.service.pq.PQRenderer;
import com.afitnerd.tnra.service.pq.PQService;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.service.SlackAPIService;
import com.afitnerd.tnra.slack.service.SlackSlashCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1")
public class SlackController {

    private SlackSlashCommandService slackSlashCommandService;
    private SlackAPIService slackAPIService;
    private PQService pqService;

    private static final String POST_COMMAND = "/post";

    private final static ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SlackController.class);

    private void doCommandAsync(SlackSlashCommandRequest request) {
        SlackSlashCommandResponse response;
        try {
            response = slackSlashCommandService.process(request);
        } catch (PostException | IllegalArgumentException e) {
            response = new SlackSlashCommandResponse();
            response.setText(e.getMessage());
        }

        try {
            StringEntity body = new StringEntity(mapper.writeValueAsString(response), ContentType.parse("utf-8"));
            Request.post(request.getResponseUrl())
                .body(body)
                .execute();
        } catch (IOException e) {
            log.error("Couldn't POST to: {} with {}", request.getResponseUrl(), request.commandString(), e);
        }
    }

    public SlackController(
        SlackSlashCommandService slackSlashCommandService,
        SlackAPIService slackAPIService,
        PQService pqService
    ) {
        this.slackSlashCommandService = slackSlashCommandService;
        this.slackAPIService = slackAPIService;
        this.pqService = pqService;
    }

    @PostMapping(
        value = POST_COMMAND,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse post(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        log.debug("slackSlashCommandRequest: {}", slackSlashCommandRequest);

        if (!slackSlashCommandService.isValidToken(slackSlashCommandRequest)) {
            throw new IllegalArgumentException("Slack token is incorrect.");
        }

        executor.execute(() -> {
            doCommandAsync(slackSlashCommandRequest);
        });

        String command = slackSlashCommandRequest.commandString();
        SlackSlashCommandResponse slackSlashCommandResponse = new SlackSlashCommandResponse();
        slackSlashCommandResponse.setText("Command received: " + command);

        return slackSlashCommandResponse;
    }

    @PostMapping(
        value = "/pq",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse pq(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        log.debug("slackSlashCommandRequest: {}", slackSlashCommandRequest);

        if (!slackSlashCommandService.isValidToken(slackSlashCommandRequest)) {
            throw new IllegalArgumentException("Slack token is incorrect.");
        }

        executor.execute(() -> {
            Map<String, PQMeResponse> metricsAll = pqService.pqMetricsAll();
            slackAPIService.chat(PQRenderer.render(metricsAll));
        });

        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        response.setResponseType(SlackSlashCommandResponse.ResponseType.EPHEMERAL);
        response.setText("PQ stats are on the way...");
        return response;
    }

    @PostMapping(
        value = "/tnra",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse help(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("help");
        return post(slackSlashCommandRequest);
    }

    @PostMapping(
        value = {"/show", "/start", "/finish", "/email"},
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sho(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String command = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText(command + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @PostMapping(
        value = {"/wid", "/kry", "/wha"},
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse wid(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String introSection = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd int " + introSection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @PostMapping(
        value = {"/per", "/fam", "/wor"},
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse per(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        String categorySection = slackSlashCommandRequest.getCommand().substring(1);
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd " + categorySection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @PostMapping(
        value = "/sta",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sta(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest) {
        slackSlashCommandRequest.setCommand(POST_COMMAND);
        slackSlashCommandRequest.setText("upd sta " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest);
    }

    @PostMapping(
        value = {"/exe", "/gtg", "/med", "/mee", "/pra", "/rea", "/spo"},
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
