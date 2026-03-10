package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationRequest;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.model.pq.PQMeResponse;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.pq.PQService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
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
        if (me == null || !StringUtils.hasText(me.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (request == null || !StringUtils.hasText(request.getLogin()) || !StringUtils.hasText(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login and password are required");
        }

        PQAuthenticationResponse response = pqService.authenticate(request.getLogin(), request.getPassword());
        User user = userRepository.findByEmail(me.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
        }
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
