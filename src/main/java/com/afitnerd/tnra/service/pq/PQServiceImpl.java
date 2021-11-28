package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.afitnerd.tnra.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
public class PQServiceImpl implements PQService {

    UserRepository userRepository;

    ObjectMapper mapper = new ObjectMapper();

    private static Logger log = LoggerFactory.getLogger(PQServiceImpl.class);

    public PQServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private PQAuthenticationResponse doAuthOperation(String json) throws IOException {
        InputStream responseStream = Request.Post(PQ_BASE_API_URL + PQ_TOKENS_URI)
                .bodyString(json, ContentType.APPLICATION_JSON)
                .execute()
                .returnContent().asStream();
        return mapper.readValue(responseStream, PQAuthenticationResponse.class);
    }

    @Override
    public void refreshAuth(User user) {
        String refreshToken = user.getPqRefreshToken();
        if (refreshToken == null) {
            log.info("No refresh token for user with id: {}", user.getId());
            return;
        }
        try {
            PQAuthenticationResponse response = refresh(refreshToken);
            user.setPqAccessToken(response.getAccessToken());
            user.setPqRefreshToken(response.getRefreshToken());
            userRepository.save(user);
            log.info("Refreshed tokens for user: {}", user.getId());
        } catch (IOException e) {
            log.error(
                "Unable to refresh tokens for user id: {}. Message: {}", user.getId(), e.getMessage(), e
            );
        }
    }

    @Override
    @Scheduled(cron = "${pq.refresh.schedule}")
    public void refreshAuthAll() {
        userRepository.findAll().forEach(this::refreshAuth);
    }

    @Override
    public PQAuthenticationResponse authenticate(String email, String password) throws IOException {
        String json = mapper.writeValueAsString(Map.of(
            "login", email,
            "password", password,
            "accept_terms_and_conditions", true
        ));
        return doAuthOperation(json);
    }

    @Override
    public PQAuthenticationResponse refresh(String refreshToken) throws IOException {
        String json = mapper.writeValueAsString(Map.of(
           "refresh_token", refreshToken
        ));
        return doAuthOperation(json);
    }

    @Override
    public PQMeResponse me(String accessToken) throws IOException {
        InputStream responseStream = Request.Get(PQ_BASE_API_URL + PQ_ME_URI)
            .addHeader("x-access-token", accessToken)
            .execute()
            .returnContent().asStream();
        return mapper.readValue(responseStream, PQMeResponse.class);
    }

    @Override
    public PQMeResponse metrics(String accessToken) throws IOException {
        InputStream responseStream = Request.Get(PQ_BASE_API_URL + PQ_METRICS_URI)
                .addHeader("x-access-token", accessToken)
                .execute()
                .returnContent().asStream();
        return mapper.readValue(responseStream, PQMeResponse.class);
    }

    private PQMeResponse getMetrics(User user, boolean retry) {
        String accessToken = user.getPqAccessToken();
        try {
            return metrics(accessToken);
        } catch (IOException e) {
            log.error(
                "Failed to retrieve pq metrics for user id: {}, Message: {}. Will refresh and retry once.",
                user.getId(), e.getMessage(), e
            );
            if (retry) {
                refreshAuth(user);
                return getMetrics(user, false);
            }
        }
        return null;
    }

    @Override
    public Map<String, PQMeResponse> pqMetricsAll() {
        Map<String, PQMeResponse> ret = new HashMap<>();
        userRepository.findAll().forEach(user -> {
            if (user.getPqAccessToken() == null) {
                log.info("No PQ access token found for user id: {}", user.getId());
                return;
            }
            PQMeResponse metrics = getMetrics(user, true);
            if (metrics != null) {
                ret.put(user.getFirstName() + " " + user.getLastName(), metrics);
            }
        });
        return ret;
    }
}
