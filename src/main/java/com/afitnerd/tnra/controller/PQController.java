package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationRequest;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.pq.PQService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
