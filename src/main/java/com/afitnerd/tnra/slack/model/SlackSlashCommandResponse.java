package com.afitnerd.tnra.slack.model;

import com.afitnerd.tnra.slack.model.attachment.Attachment;
import com.fasterxml.jackson.annotation.JsonInclude;


import java.util.ArrayList;
import java.util.List;

public class SlackSlashCommandResponse {

    private String text;

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
}

