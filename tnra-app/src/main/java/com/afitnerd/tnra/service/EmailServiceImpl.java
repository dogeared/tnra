package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailServiceImpl implements EMailService {

    @Value("#{ @environment['mailgun.key.private'] }")
    private String mailgunPrivateKey;

    @Value("#{ @environment['mailgun.key.public'] }")
    private String mailgunPublicKey;

    @Value("#{ @environment['mailgun.url'] }")
    private String mailgunUrl;

    @Value("${tnra.emailService.enabled:true}")
    private boolean emailServiceEnabled;

    private final PostRenderer postRenderer;
    private final UserRepository userRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<HashMap<String,Object>> typeRef = new TypeReference<>() {};

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    public EmailServiceImpl(
        @Qualifier("activityNotificationRenderer") PostRenderer postRenderer,
        UserRepository userRepository
    ) {
        this.postRenderer = postRenderer;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void setup() {
        log.info("Mailgun - public key: {}, url: {}", mailgunPublicKey, mailgunUrl);
    }

    @Override
    public void sendMailToMe(User user, Post post) {
        if (!emailServiceEnabled) {
            log.debug("Email service disabled — skipping email to {}", user.getEmail());
            return;
        }

        try {
            HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("from", "bot@tnra.afitnerd.com")
                .addTextBody("to", user.getEmail())
                .addTextBody(
                    "subject",
                    "TNRA: New activity from " +
                        (post.getUser().getFirstName() != null ? post.getUser().getFirstName() : "a member")
                )
                .addTextBody(
                    "html",
                    postRenderer.render(post),
                    ContentType.create("text/html", StandardCharsets.UTF_8)
                )
                .build();

            try (InputStream responseStream = Request.post(mailgunUrl)
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunPrivateKey).getBytes("utf-8")))
                .body(entity)
                .execute().returnContent().asStream()) {
                Map<String, Object> response = mapper.readValue(responseStream, typeRef);
                log.info(
                    "Sent email to {}. Got Mailgun response: {}",
                    user.getEmail(), mapper.writeValueAsString(response)
                );
            }
        } catch (IOException e) {
            log.error("Error sending Mailgun email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendMailToAll(Post post) {
        if (!emailServiceEnabled) {
            log.debug("Email service disabled — skipping all notifications");
            return;
        }

        userRepository.findByActiveTrue().forEach(user -> {
            if (Boolean.TRUE.equals(user.getNotifyNewPosts())) {
                sendMailToMe(user, post);
            } else {
                log.debug("Skipping email to {} — notifications disabled", user.getEmail());
            }
        });
    }
}
