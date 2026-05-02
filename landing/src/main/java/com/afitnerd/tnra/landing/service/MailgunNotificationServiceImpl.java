package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MailgunNotificationServiceImpl implements MailgunNotificationService {

    private static final Logger log = LoggerFactory.getLogger(MailgunNotificationServiceImpl.class);

    @Value("${mailgun.key.private:}")
    private String mailgunPrivateKey;

    @Value("${mailgun.url:}")
    private String mailgunUrl;

    @Value("${tnra.notify.founder-email}")
    private String founderEmail;

    @Value("${tnra.notify.enabled:true}")
    private boolean notifyEnabled;

    @Override
    public void notifyFounder(RequestAccess request) {
        if (!notifyEnabled || mailgunUrl.isBlank() || mailgunPrivateKey.isBlank()) {
            log.info("Mailgun notification skipped (disabled or not configured) for request from {}", request.getEmail());
            return;
        }

        String body = buildBody(request);

        try {
            HttpEntity entity = MultipartEntityBuilder.create()
                .addTextBody("from", "tnra-landing@tnra.app")
                .addTextBody("to", founderEmail)
                .addTextBody("subject", "New TNRA access request: " + request.getGroupName())
                .addTextBody("html", body, ContentType.create("text/html", StandardCharsets.UTF_8))
                .build();

            Request.post(mailgunUrl)
                .addHeader("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString(("api:" + mailgunPrivateKey).getBytes(StandardCharsets.UTF_8)))
                .body(entity)
                .execute().returnContent();

            log.info("Notified founder of access request from {} / {}", request.getGroupName(), request.getEmail());
        } catch (IOException e) {
            log.error("Failed to send Mailgun notification for request from {}: {}", request.getEmail(), e.getMessage(), e);
        }
    }

    private String buildBody(RequestAccess r) {
        return "<h2>New TNRA Access Request</h2>" +
            "<table style='border-collapse:collapse;font-family:sans-serif'>" +
            row("Group Name", r.getGroupName()) +
            row("Contact", r.getContactName()) +
            row("Email", "<a href='mailto:" + r.getEmail() + "'>" + r.getEmail() + "</a>") +
            row("Estimated Size", r.getEstimatedSize() != null ? String.valueOf(r.getEstimatedSize()) : "—") +
            row("Submitted", r.getSubmittedAt().toString()) +
            row("IP", r.getIpAddress() != null ? r.getIpAddress() : "—") +
            "</table>" +
            (r.getDescription() != null && !r.getDescription().isBlank()
                ? "<h3>About the group</h3><p style='font-family:sans-serif'>" + escapeHtml(r.getDescription()) + "</p>"
                : "");
    }

    private String row(String label, String value) {
        return "<tr><td style='padding:6px 12px 6px 0;font-weight:600;color:#6B5D4F'>" + label + "</td>" +
               "<td style='padding:6px 0;color:#2C2418'>" + value + "</td></tr>";
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
