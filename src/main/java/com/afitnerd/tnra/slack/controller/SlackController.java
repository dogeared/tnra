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
        value = "/slack", method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
    )
    public @ResponseBody
    SlackSlashCommandResponse slack(@RequestBody SlackSlashCommandRequest slackSlashCommandRequest, HttpServletRequest httpServletRequest) {
        log.debug("slackSlashCommandRequest: {}", slackSlashCommandRequest);

        httpServletRequest.setAttribute(SLACK_COMMAND_ATTRIBUTE, slackSlashCommandRequest);

        if (!slackSlashCommandService.isValidToken(slackSlashCommandRequest)) {
            throw new IllegalArgumentException("Slack token is incorrect.");
        }

        return slackSlashCommandService.process(slackSlashCommandRequest);
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
