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
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
            StringEntity body = new StringEntity(mapper.writeValueAsString(response), "utf-8");
            Request.Post(request.getResponseUrl())
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

        executor.execute(() -> {
            doCommandAsync(slackSlashCommandRequest);
        });

        String command = slackSlashCommandRequest.commandString();
        SlackSlashCommandResponse slackSlashCommandResponse = new SlackSlashCommandResponse();
        slackSlashCommandResponse.setText("Command received: " + command);

        return slackSlashCommandResponse;
    }

    @RequestMapping(
        value = "/pq", method = RequestMethod.POST,
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
        value = {"/show", "/start", "/finish", "/email"}, method = RequestMethod.POST,
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
