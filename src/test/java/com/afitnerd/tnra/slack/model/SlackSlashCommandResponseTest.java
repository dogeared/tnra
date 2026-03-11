package com.afitnerd.tnra.slack.model;

import com.afitnerd.tnra.slack.model.attachment.BasicAttachment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlackSlashCommandResponseTest {

    @Test
    void responseTypeEnumMapsKnownValuesAndRejectsUnknownOnes() {
        assertEquals(SlackSlashCommandResponse.ResponseType.EPHEMERAL,
            SlackSlashCommandResponse.ResponseType.fromValue("ephemeral"));
        assertEquals(SlackSlashCommandResponse.ResponseType.IN_CHANNEL,
            SlackSlashCommandResponse.ResponseType.fromValue("in_channel"));
        assertThrows(IllegalArgumentException.class,
            () -> SlackSlashCommandResponse.ResponseType.fromValue("unknown"));
    }

    @Test
    void responseStoresTextAttachmentsAndResponseType() {
        SlackSlashCommandResponse response = new SlackSlashCommandResponse();
        BasicAttachment attachment = new BasicAttachment("Title", "Text");

        response.setText("done");
        response.addAttachment(attachment);
        response.setResponseType(SlackSlashCommandResponse.ResponseType.IN_CHANNEL);

        assertEquals("done", response.getText());
        assertEquals(1, response.getAttachments().size());
        assertEquals(attachment, response.getAttachments().getFirst());
        assertEquals("in_channel", response.getResponseType());
    }
}
