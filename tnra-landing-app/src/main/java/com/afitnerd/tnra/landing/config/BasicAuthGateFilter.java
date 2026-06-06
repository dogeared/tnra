package com.afitnerd.tnra.landing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Gates the entire landing app behind a single HTTP Basic username/password.
 *
 * Used to password-protect a production deploy for private review before the site
 * goes public. Runs as a plain servlet filter ahead of Spring Security so it gates
 * everything — Vaadin bootstrap, push handshake, static resources — regardless of
 * the {@code @AnonymousAllowed} views. The browser sends cached credentials on all
 * subsequent same-origin requests (including the WebSocket handshake), so Vaadin
 * keeps working once the reviewer authenticates.
 *
 * Only registered when {@code tnra.basic-auth.enabled=true} (see {@link BasicAuthConfig}).
 */
public class BasicAuthGateFilter extends OncePerRequestFilter {

    private static final String BASIC_PREFIX = "Basic ";

    private final String username;
    private final String password;
    private final String realm;

    public BasicAuthGateFilter(String username, String password, String realm) {
        this.username = username;
        this.password = password;
        this.realm = realm;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isAuthorized(request.getHeader("Authorization"))) {
            chain.doFilter(request, response);
            return;
        }
        // 401 + WWW-Authenticate triggers the browser's native login prompt. Set the
        // status directly (not sendError) so the header survives and no error page
        // dispatch is triggered.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\", charset=\"UTF-8\"");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Authentication required");
    }

    boolean isAuthorized(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return false;
        }
        String decoded;
        try {
            byte[] raw = Base64.getDecoder().decode(authorizationHeader.substring(BASIC_PREFIX.length()).trim());
            decoded = new String(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException badBase64) {
            return false;
        }
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            return false;
        }
        String providedUser = decoded.substring(0, separator);
        String providedPass = decoded.substring(separator + 1);
        // Non-short-circuiting & so both comparisons always run (timing-attack resistant).
        return secureEquals(providedUser, username) & secureEquals(providedPass, password);
    }

    private static boolean secureEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
