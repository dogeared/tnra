package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailServiceImpl implements EMailService {

    @Value("#{ @environment['mailgun.key.private'] }")
    private String mailgunPrivateKey;

    @Value("#{ @environment['mailgun.key.public'] }")
    private String mailgunPublicKey;

    @Value("#{ @environment['mailgun.url'] }")
    private String mailgunUrl;

    private PostRenderer emailPostRenderer;
    private UserRepository userRepository;

    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    private static Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    public EmailServiceImpl(
        @Qualifier("emailPostRenderer") PostRenderer eMailPostRenderer,
        UserRepository userRepository
    ) {
        this.emailPostRenderer = eMailPostRenderer;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void setup() {
        log.info("Mailgun - public key: {}, url: {}", mailgunPublicKey, mailgunUrl);
    }

    @Override
    public void sendMailToMe(User user, Post post) {
        Runnable runnableTask = () -> {
            try {
                HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .addTextBody("from", "bot@tnra.afitnerd.com")
                    .addTextBody("to", user.getEmail())
                    .addTextBody(
                        "subject",
                        "Post From " + post.getUser().getFirstName() + " " + post.getUser().getLastName()
                    )
                    .addTextBody(
                        "html",
                        emailPostRenderer.render(post),
                        ContentType.create("text/hmtl", StandardCharsets.UTF_8)
                    )
                    .build();

                InputStream responseStream = Request.Post(mailgunUrl)
                    .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunPrivateKey).getBytes("utf-8")))
                    .body(entity)
                    .execute().returnContent().asStream();
                Map<String, Object> response =  mapper.readValue(responseStream, typeRef);
                log.info(
                    "Sent email to {}. Got Mailgun response: {}",
                    user.getEmail(), mapper.writeValueAsString(response)
                );
            } catch (IOException e) {
                log.error("Error sending Mailgun email: {}", e.getMessage(), e);
            }
        };

        executor.execute(runnableTask);
    }

    @Override
    public void sendMailToAll(Post post) {
        userRepository.findAll().forEach(user -> {
            sendMailToMe(user, post);
        });
    }

    @Override
    public void sendTextViaMail(User user, Post post) {
        Runnable runnableTask = () -> {
            try {
                User postUser = post.getUser();
                HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .addTextBody("from", "bot@tnra.afitnerd.com")
                    .addTextBody("to", user.getPhoneNumber() + "@" + user.getTextEmailSuffix())
                    .addTextBody(
                        "subject",
                        postUser.getFirstName() + " " + postUser.getLastName()
                    )
                    .addTextBody("text", "\n" + PostRenderer.utf8ToAscii(post.getIntro().getWhatAndWhen()))
                    .build();
                InputStream responseStream = Request.Post(mailgunUrl)
                    .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunPrivateKey).getBytes("utf-8")))
                    .body(entity)
                    .execute().returnContent().asStream();
                Map<String, Object> response =  mapper.readValue(responseStream, typeRef);
                log.info(
                    "Sent text to {}@{}. Got Mailgun response: {}",
                    user.getPhoneNumber(), user.getTextEmailSuffix(), mapper.writeValueAsString(response)
                );
            } catch (IOException e) {
                log.error("Error sending Mailgun email: {}", e.getMessage(), e);
            }
        };
        executor.execute(runnableTask);
    }
}