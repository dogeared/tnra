package com.afitnerd.tnra.slack.model.attachment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BasicAttachmentTest {

    @Test
    void twoArgumentConstructorSetsTitleAndTextOnly() {
        BasicAttachment attachment = new BasicAttachment("Title", "Body");

        assertEquals("Title", attachment.getTitle());
        assertEquals("Body", attachment.getText());
        assertNull(attachment.getPretext());
        assertNull(attachment.getMarkdownIn());
    }

    @Test
    void threeArgumentConstructorSetsPretext() {
        BasicAttachment attachment = new BasicAttachment("Title", "Pre", "Body");

        assertEquals("Pre", attachment.getPretext());
        assertNull(attachment.getMarkdownIn());
    }

    @Test
    void varargsConstructorLowerCasesMarkdownEnums() {
        BasicAttachment attachment = new BasicAttachment(
            "Title",
            "Pre",
            "Body",
            Attachment.MarkdownIn.PRETEXT,
            Attachment.MarkdownIn.TEXT
        );

        assertEquals(List.of("pretext", "text"), attachment.getMarkdownIn());
    }

    @Test
    void varargsConstructorAllowsNullMarkdownList() {
        BasicAttachment attachment = new BasicAttachment("Title", "Pre", "Body", (Attachment.MarkdownIn[]) null);

        assertNull(attachment.getMarkdownIn());
    }
}
