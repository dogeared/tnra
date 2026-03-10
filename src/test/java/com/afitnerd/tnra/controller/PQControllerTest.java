package com.afitnerd.tnra.controller;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.model.pq.PQAuthenticationRequest;
import com.afitnerd.tnra.model.pq.PQAuthenticationResponse;
import com.afitnerd.tnra.repository.UserRepository;
import com.afitnerd.tnra.service.pq.PQService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PQControllerTest {

    @Mock
    private PQService pqService;

    @Mock
    private UserRepository userRepository;

    private PQController controller;

    @BeforeEach
    void setUp() {
        controller = new PQController(pqService, userRepository);
    }

    @Test
    void pqAuthenticateRejectsMissingCredentials() throws IOException {
        Principal principal = () -> "user@example.com";
        PQAuthenticationRequest request = new PQAuthenticationRequest();
        request.setLogin(" ");
        request.setPassword("secret");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> controller.pqAuthenticate(principal, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(pqService, never()).authenticate(anyString(), anyString());
    }

    @Test
    void pqAuthenticateRejectsUnknownAuthenticatedUser() throws IOException {
        Principal principal = () -> "user@example.com";
        PQAuthenticationRequest request = new PQAuthenticationRequest();
        request.setLogin("login");
        request.setPassword("password");

        PQAuthenticationResponse response = new PQAuthenticationResponse();
        response.setAccessToken("access-token");
        response.setRefreshToken("refresh-token");
        when(pqService.authenticate("login", "password")).thenReturn(response);
        when(userRepository.findByEmail("user@example.com")).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> controller.pqAuthenticate(principal, request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void pqAuthenticateStoresTokensForValidRequest() throws IOException {
        Principal principal = () -> "user@example.com";
        User user = new User();
        user.setEmail("user@example.com");

        PQAuthenticationRequest request = new PQAuthenticationRequest();
        request.setLogin("login");
        request.setPassword("password");

        PQAuthenticationResponse response = new PQAuthenticationResponse();
        response.setAccessToken("access-token");
        response.setRefreshToken("refresh-token");

        when(pqService.authenticate("login", "password")).thenReturn(response);
        when(userRepository.findByEmail("user@example.com")).thenReturn(user);

        Map<String, String> result = controller.pqAuthenticate(principal, request);

        assertEquals("SUCCESS", result.get("status"));
        assertEquals("access-token", user.getPqAccessToken());
        assertEquals("refresh-token", user.getPqRefreshToken());
        verify(userRepository).save(user);
    }
}
