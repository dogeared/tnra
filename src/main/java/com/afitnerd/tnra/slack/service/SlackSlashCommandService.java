package com.afitnerd.tnra.slack.service;

import com.afitnerd.tnra.slack.model.SlackSlashCommandRequest;
import com.afitnerd.tnra.slack.model.SlackSlashCommandResponse;

public interface SlackSlashCommandService {

    boolean isValidToken(SlackSlashCommandRequest request);
    SlackSlashCommandResponse process(SlackSlashCommandRequest request);
}
