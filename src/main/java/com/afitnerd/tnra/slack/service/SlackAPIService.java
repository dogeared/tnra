package com.afitnerd.tnra.slack.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.Map;

public interface SlackAPIService {

    String SLACK_BASE_URL = "https://slack.com/api";
    String SLACK_CHAT_POST_MESSAGE = "/chat.postMessage";

    Map<String, Object> chat(String text);
}
