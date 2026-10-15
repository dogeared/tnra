package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.MailgunProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal Mailgun sender (multipart POST to the messages endpoint, HTTP Basic {@code api:<key>}) on the
 * same {@link RestClient} the Paddle client uses — no extra dependency. Throws on failure; callers decide
 * whether that aborts their unit of work.
 */
@Service
public class MailgunMailer {

    private static final Logger log = LoggerFactory.getLogger(MailgunMailer.class);

    private final RestClient restClient;
    private final MailgunProperties props;

    public MailgunMailer(RestClient.Builder builder, MailgunProperties props) {
        this.restClient = builder.build();
        this.props = props;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public void send(String from, String to, String subject, String html) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);
        form.add("html", html);
        String auth = Base64.getEncoder()
            .encodeToString(("api:" + props.getKeyPrivate()).getBytes(StandardCharsets.UTF_8));
        restClient.post()
            .uri(props.getUrl())
            .header("Authorization", "Basic " + auth)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(form)
            .retrieve()
            .toBodilessEntity();
        log.info("Sent \"{}\" to {}", subject, to);
    }
}
