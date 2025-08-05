package com.afitnerd.tnra.slack.model;

import com.afitnerd.tnra.slack.model.attachment.Attachment;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class SlackSlashCommandResponse {

    public enum ResponseType {
        EPHEMERAL("ephemeral"),
        IN_CHANNEL("in_channel");

        private String value;

        ResponseType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ResponseType fromValue(String value) {
            switch (value) {
                case "ephemeral":
                    return EPHEMERAL;
                case "in_channel":
                    return IN_CHANNEL;
                default:
                    throw new IllegalArgumentException(value + " is not a valid ResponseType.");
            }
        }
    }

    private String text;

    @JsonProperty("response_type")
    private String responseType = "ephemeral";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Attachment> attachments;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        attachments.add(attachment);
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType.value;
    }
}

