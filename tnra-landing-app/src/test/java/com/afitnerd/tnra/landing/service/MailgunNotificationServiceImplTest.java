package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailgunNotificationServiceImplTest {

    private MailgunNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MailgunNotificationServiceImpl();
        ReflectionTestUtils.setField(service, "mailgunPrivateKey", "key-test");
        ReflectionTestUtils.setField(service, "mailgunUrl", "http://127.0.0.1:1/v3/mail.tnra.app/messages");
        ReflectionTestUtils.setField(service, "founderEmail", "founder@example.com");
        ReflectionTestUtils.setField(service, "notifyEnabled", true);
        ReflectionTestUtils.setField(service, "fromAddress", "tnra-landing@tnra.app");
    }

    private static RequestAccess request(String description) {
        RequestAccess r = new RequestAccess();
        r.setGroupName("The Bobs");
        r.setContactName("Bob Slydell");
        r.setEmail("bob@example.com");
        r.setEstimatedSize(8);
        r.setDescription(description);
        r.setSubmittedAt(LocalDateTime.of(2026, 10, 14, 23, 13));
        r.setIpAddress("203.0.113.9");
        return r;
    }

    private static String content(HttpEntity entity) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void message_usesConfiguredFromAddress_andFounderRecipient() throws Exception {
        ReflectionTestUtils.setField(service, "fromAddress", "notify@mail.tnra.app");

        String body = content(service.buildEntity(request("We meet weekly.")));

        assertTrue(body.contains("name=\"from\"") && body.contains("notify@mail.tnra.app"), "configured From");
        assertFalse(body.contains("tnra-landing@tnra.app"), "default From must not leak when overridden");
        assertTrue(body.contains("founder@example.com"), "To = founder");
        assertTrue(body.contains("New TNRA access request: The Bobs"), "subject names the group");
        assertTrue(body.contains("bob@example.com") && body.contains("Bob Slydell") && body.contains("203.0.113.9"));
        assertTrue(body.contains("We meet weekly."), "description section present");
    }

    @Test
    void message_defaultsToHistoricalFrom_andOmitsDescriptionSectionWhenBlank() throws Exception {
        String body = content(service.buildEntity(request("  ")));

        assertTrue(body.contains("tnra-landing@tnra.app"));
        assertFalse(body.contains("About the group"));
    }

    @Test
    void message_escapesHtmlInTheDescription() throws Exception {
        String body = content(service.buildEntity(request("<script>alert(1)</script> & co")));

        assertTrue(body.contains("&lt;script&gt;"), "angle brackets escaped");
        assertTrue(body.contains("&amp; co"), "ampersand escaped");
        assertFalse(body.contains("<script>"));
    }

    @Test
    void notifyFounder_isSkipped_whenDisabledOrUnconfigured() {
        ReflectionTestUtils.setField(service, "notifyEnabled", false);
        assertDoesNotThrow(() -> service.notifyFounder(request("x")));

        ReflectionTestUtils.setField(service, "notifyEnabled", true);
        ReflectionTestUtils.setField(service, "mailgunUrl", "");
        assertDoesNotThrow(() -> service.notifyFounder(request("x")));

        ReflectionTestUtils.setField(service, "mailgunUrl", "http://127.0.0.1:1/messages");
        ReflectionTestUtils.setField(service, "mailgunPrivateKey", "");
        assertDoesNotThrow(() -> service.notifyFounder(request("x")));
    }

    @Test
    void notifyFounder_swallowsTransportFailures_soTheRequestIsStillSaved() {
        // port 1 refuses the connection: the IOException path must log, not propagate.
        assertDoesNotThrow(() -> service.notifyFounder(request("x")));
    }
}
