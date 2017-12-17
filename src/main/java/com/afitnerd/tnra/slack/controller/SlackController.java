package com.afitnerd.tnra.slack.controller;

import com.afitnerd.tnra.exception.PostException;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.service.SlackSlashCommandService;
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

@RestController
@RequestMapping("/api/v1")
public class SlackController {

    private static final String SLACK_COMMAND_ATTRIBUTE = "slack-command";

    private SlackSlashCommandService slackSlashCommandService;
    private static final Logger log = LoggerFactory.getLogger(SlackController.class);

    public SlackController(SlackSlashCommandService slackSlashCommandService) {
        this.slackSlashCommandService = slackSlashCommandService;
    }

    @RequestMapping(
        value = "/post", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse post(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        log.debug("slackSlashCommandRequest: {}", slackSlashCommandRequest);

        httpServletRequest.setAttribute(SLACK_COMMAND_ATTRIBUTE, slackSlashCommandRequest);

        if (!slackSlashCommandService.isValidToken(slackSlashCommandRequest)) {
            throw new IllegalArgumentException("Slack token is incorrect.");
        }

        return slackSlashCommandService.process(slackSlashCommandRequest);
    }

    @RequestMapping(
        value = {"/wid", "/kry", "/wha"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse wid(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        String introSection = httpServletRequest.getRequestURI().substring(httpServletRequest.getRequestURI().lastIndexOf("/") + 1);
        slackSlashCommandRequest.setText("upd int " + introSection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest, httpServletRequest);
    }

    @RequestMapping(
        value = {"/per", "/fam", "/wor"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse per(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        String categorySection = httpServletRequest.getRequestURI().substring(httpServletRequest.getRequestURI().lastIndexOf("/") + 1);
        slackSlashCommandRequest.setText("upd " + categorySection + " " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest, httpServletRequest);
    }

    @RequestMapping(
        value = "/sta", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sta(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        slackSlashCommandRequest.setText("upd sta " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest, httpServletRequest);
    }

    @RequestMapping(
        value = {"/exe", "/gtg", "/med", "/mee", "/pra", "/rea", "/spo"}, method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse staSpecific(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        String stat = httpServletRequest.getRequestURI().substring(httpServletRequest.getRequestURI().lastIndexOf("/") + 1);
        slackSlashCommandRequest.setText("upd sta " + stat + ":" + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest, httpServletRequest);
    }

    @RequestMapping(
        value = "/sho", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse sho(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        slackSlashCommandRequest.setText("sho " + slackSlashCommandRequest.getText());
        return post(slackSlashCommandRequest, httpServletRequest);
    }

    @ExceptionHandler({IllegalArgumentException.class, PostException.class})
    public SlackSlashCommandResponse handleException(Exception e, HttpServletRequest httpServletRequest) {
        String msg = e.getMessage();
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        // try to retrieve original slack request
        SlackSlashCommandRequest slackSlashCommandRequest =
            (SlackSlashCommandRequest) httpServletRequest.getAttribute(SLACK_COMMAND_ATTRIBUTE);
        if (slackSlashCommandRequest != null) {
            msg = slackSlashCommandRequest.commandString() + msg;
        }
        response.setText(msg);
        return response;
    }
}
