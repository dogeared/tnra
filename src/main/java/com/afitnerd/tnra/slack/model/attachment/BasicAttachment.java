package com.afitnerd.tnra.slack.model.attachment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BasicAttachment implements Attachment {

    private final String title;
    private final String pretext;
    private final String text;
    private final List<String> markdownIn;

    public BasicAttachment(String title, String text) {
        this.title = title;
        this.text = text;
        this.pretext = null;
        this.markdownIn = null;
    }

    public BasicAttachment(String title, String pretext, String text) {
        this.title = title;
        this.text = text;
        this.pretext = pretext;
        this.markdownIn = null;
    }

    public BasicAttachment(String title, String pretext, String text, MarkdownIn... markdownIns) {
        this.title = title;
        this.text = text;
        this.pretext = pretext;
        if (markdownIns != null) {
            markdownIn = new ArrayList<>();
            Stream.of(markdownIns).forEach(e -> markdownIn.add(e.name().toLowerCase()));
        } else {
            markdownIn = null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Override
    public String getTitle() {
        return this.title;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Override
    public String getPretext() {
        return pretext;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("mrkdwn_in")
    @Override
    public List<String> getMarkdownIn() {
        return markdownIn;
    }
}
