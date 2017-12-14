package com.afitnerd.tnra.slack.model.attachment;

import java.util.List;

public interface Attachment {

    String getTitle();
    String getPretext();
    String getText();
    List<String> getMarkdownIn();

    enum MarkdownIn {
        TEXT, PRETEXT;
    }
}
