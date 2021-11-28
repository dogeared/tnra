package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationRequest;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.pq.PQService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PQController {

    private PQService pqService;
    private UserRepository userRepository;

    ObjectMapper mapper = new ObjectMapper();

    private static Logger log = LoggerFactory.getLogger(PQController.class);

    public PQController(PQService pqService, UserRepository userRepository) {
        this.pqService = pqService;
        this.userRepository = userRepository;
    }

    @ExceptionHandler({HttpResponseException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleException(HttpResponseException exception) {
        return Map.of("status", "FAILURE");
    }

    @PostMapping("/pq_authenticate")
    public Map<String, String> pqAuthenticate(
        Principal me, @RequestBody PQAuthenticationRequest request
    ) throws IOException {
        // TODO - check presence of login and password
        PQAuthenticationResponse response = pqService.authenticate(request.getLogin(), request.getPassword());
//        log.info("pq auth: {}", mapper.writeValueAsString(response));
        User user = userRepository.findByEmail(me.getName());
        user.setPqAccessToken(response.getAccessToken());
        user.setPqRefreshToken(response.getRefreshToken());
        userRepository.save(user);
        return Map.of("status", "SUCCESS");
    }

    @GetMapping("/pq_is_authenticated")
    public Map<String, Object> isAuthenticated(Principal me) throws IOException {
        User user = userRepository.findByEmail(me.getName());
        return Map.of("is_authenticated", user.getPqAccessToken() != null);
    }

    @GetMapping("/pq_metrics_me")
    public PQMeResponse pqMe(Principal me) throws IOException {
        User user = userRepository.findByEmail(me.getName());
        if (user.getPqAccessToken() != null) {
            return pqService.metrics(user.getPqAccessToken());
        }
        log.info("No PQ access token found for user id: {}", user.getId());
        return null;
    }

    @GetMapping("/pq_metrics_all")
    public Map<String, PQMeResponse> pqMetricsAll() {
        return pqService.pqMetricsAll();
    }
}
