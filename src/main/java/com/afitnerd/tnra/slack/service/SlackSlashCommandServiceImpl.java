package com.afitnerd.tnra.slack.service;

import com.afitnerd.tnra.model.command.Action;
import com.afitnerd.tnra.model.command.Command;
import com.afitnerd.tnra.service.PostService;
import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;
import com.afitnerd.tnra.slack.model.attachment.Attachment;
import com.afitnerd.tnra.slack.model.attachment.BasicAttachment;
import com.afitnerd.tnra.util.CommandParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class SlackSlashCommandServiceImpl implements SlackSlashCommandService {

    @Value("#{ @environment['tnra.slack.token'] ?: '' }")
    private String slackVerificationToken;

    private PostService postService;

    public SlackSlashCommandServiceImpl(PostService postService) {
        this.postService = postService;
    }

    @PostConstruct
    protected void setup() {
    }

    @Override
    public boolean isValidToken(SlackSlashCommandRequest request) {
        return slackVerificationToken.equals(request.getToken());
    }

    @Override
    public SlackSlashCommandResponse process(SlackSlashCommandRequest request) {
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        Command command = CommandParser.parse(request.getText());
        if (Action.HELP.equals(command.getAction())) {
            response.setText("```" + CommandParser.help() + "```");
        } else {
            response.setText(command.toString());
        }
        return response;
    }


    private SlackSlashCommandResponse help() {
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        response.setText("Help:");
        BasicAttachment attachment = new BasicAttachment(null, null, "placeholder", Attachment.MarkdownIn.TEXT);
        response.addAttachment(attachment);
        return response;
    }
}
