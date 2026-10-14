package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.MailgunProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MailgunMailerTest {

    private static final String URL = "https://api.mailgun.net/v3/mail.tnra.app/messages";

    private MailgunProperties props(String key, String url) {
        MailgunProperties p = new MailgunProperties();
        p.setKeyPrivate(key);
        p.setUrl(url);
        return p;
    }

    @Test
    void send_postsMultipartWithBasicAuth() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MailgunMailer mailer = new MailgunMailer(builder, props("key-123", URL));
        server.expect(requestTo(URL))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("api:key-123".getBytes())))
            .andExpect(header("Content-Type", org.hamcrest.Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"to\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("m@x.com")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<p>hi</p>")))
            .andRespond(withSuccess("{\"id\":\"<x@mail.tnra.app>\",\"message\":\"Queued. Thank you.\"}", MediaType.APPLICATION_JSON));

        mailer.send("from@mail.tnra.app", "m@x.com", "Subject", "<p>hi</p>");
        server.verify();
    }

    @Test
    void send_propagatesProviderErrors() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MailgunMailer mailer = new MailgunMailer(builder, props("key", URL));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN).body("not allowed to send"));

        assertThrows(RestClientResponseException.class, () -> mailer.send("f@x", "t@x", "s", "b"));
    }

    @Test
    void isConfigured_needsBothKeyAndUrl() {
        assertTrue(new MailgunMailer(RestClient.builder(), props("k", URL)).isConfigured());
        assertFalse(new MailgunMailer(RestClient.builder(), props("", URL)).isConfigured());
        assertFalse(new MailgunMailer(RestClient.builder(), props("k", "")).isConfigured());
    }
}
