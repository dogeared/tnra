package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
public class PQServiceImpl implements PQService {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public PQAuthenticationResponse authenticate(String email, String password) throws IOException {
        String json = mapper.writeValueAsString(Map.of(
            "login", email,
            "password", password,
            "accept_terms_and_conditions", true
        ));
        InputStream responseStream = Request.Post(PQ_BASE_API_URL + PQ_TOKENS_URI)
            .bodyString(json, ContentType.APPLICATION_JSON)
            .execute()
            .returnContent().asStream();
        return mapper.readValue(responseStream, PQAuthenticationResponse.class);
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
}
