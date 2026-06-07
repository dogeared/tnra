package com.afitnerd.tnra.landing.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthGateFilterTest {

    private final BasicAuthGateFilter filter = new BasicAuthGateFilter("admin", "s3cret", "TNRA Preview");

    private static String basic(String user, String pass) {
        String token = Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private MockHttpServletResponse run(String authHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void correctCredentialsPassThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.addHeader("Authorization", basic("admin", "s3cret"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // chain proceeded → the request reached the (mock) downstream resource
        assertNotNull(chain.getRequest(), "filter chain should have continued");
        assertEquals(200, response.getStatus());
    }

    @Test
    void missingHeaderChallengesWith401() throws Exception {
        MockHttpServletResponse response = run(null);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Basic realm=\"TNRA Preview\""));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, run(basic("admin", "nope")).getStatus());
    }

    @Test
    void wrongUsernameIsRejected() throws Exception {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, run(basic("root", "s3cret")).getStatus());
    }

    @Test
    void nonBasicSchemeIsRejected() throws Exception {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, run("Bearer sometoken").getStatus());
    }

    @Test
    void malformedBase64IsRejected() throws Exception {
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, run("Basic !!!not-base64!!!").getStatus());
    }

    @Test
    void credentialsWithoutColonAreRejected() throws Exception {
        String token = Base64.getEncoder().encodeToString("nocolon".getBytes(StandardCharsets.UTF_8));
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, run("Basic " + token).getStatus());
    }

    @Test
    void passwordContainingColonIsHandled() throws Exception {
        BasicAuthGateFilter colonPass = new BasicAuthGateFilter("admin", "a:b:c", "TNRA Preview");
        assertTrue(colonPass.isAuthorized(basic("admin", "a:b:c")));
    }

    @Test
    void schemeIsCaseInsensitive() throws Exception {
        String token = Base64.getEncoder().encodeToString("admin:s3cret".getBytes(StandardCharsets.UTF_8));
        assertTrue(filter.isAuthorized("basic " + token));
    }

    @Test
    void nullHeaderIsNotAuthorized() {
        assertFalse(filter.isAuthorized(null));
    }
}
