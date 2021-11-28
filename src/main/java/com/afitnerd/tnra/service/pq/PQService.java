package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.Map;

public interface PQService {

    String PQ_API_VERSION = "v1.4";
    String PQ_BASE_API_URL = "https://api.positiveintelligence.com/" + PQ_API_VERSION;
    String PQ_TOKENS_URI = "/auth/tokens";
    String PQ_ME_URI = "/users/me";
    String PQ_METRICS_URI = PQ_ME_URI + "/metrics";

    void refreshAuth(User user);
    void refreshAuthAll();

    PQAuthenticationResponse authenticate(String email, String password) throws IOException;
    PQAuthenticationResponse refresh(String refreshToken) throws IOException;
    PQMeResponse me(String accessToken) throws IOException;
    PQMeResponse metrics(String accessToken) throws IOException;

    Map<String, PQMeResponse> pqMetricsAll();
}
