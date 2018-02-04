package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

    private PostRenderer emailPostRenderer;

    private ObjectMapper mapper = new ObjectMapper();
    private TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};

    private static Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    public EmailServiceImpl(@Qualifier("emailPostRenderer") PostRenderer eMailPostRenderer) {
        this.emailPostRenderer = eMailPostRenderer;
    }

    public void sendMailToMe(User user, Post post) {
        user.setEmail("micah@afitnerd.com");
        try {
            InputStream responseStream = Request.Post(mailgunUrl)
                .addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunPrivateKey).getBytes("utf-8")))
                .bodyForm(Form.form()
                    .add("from", "bot@tnra.afitnerd.com")
                    .add("to", user.getEmail())
                    .add("subject", "Post From " + post.getUser().getFirstName() + " " + post.getUser().getLastName())
                    .add("html", emailPostRenderer.render(post))
                    .build()
                )
                .execute().returnContent().asStream();
            Map<String, Object> response =  mapper.readValue(responseStream, typeRef);
            log.info("Sent email to {}. Got Mailgun response: {}", user.getEmail(), mapper.writeValueAsString(response));
        } catch (IOException e) {
            log.error("Error sending Mailgun email: {}", e.getMessage(), e);
        }
    }
}