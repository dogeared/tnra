package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface PQService {

    String PQ_API_VERSION = "v1.4";
    String PQ_BASE_API_URL = "https://api.positiveintelligence.com/" + PQ_API_VERSION;
    String PQ_TOKENS_URI = "/auth/tokens";

    PQAuthenticationResponse authenticate(String email, String password) throws IOException;
}
