package com.afitnerd.tnra.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression coverage for the 30-day "stay logged in" behavior.
 *
 * The bug: only server.servlet.session.timeout was set, so JSESSIONID had no
 * max-age and was a browser-session cookie — dropped when the browser closed,
 * logging users out long before the 30-day server session (and Keycloak SSO
 * session) expired. The cookie max-age must match the session timeout so the
 * cookie persists for the full 30 days.
 */
@SpringBootTest
public class SessionConfigTest {

    @Autowired
    private ServerProperties serverProperties;

    @Test
    public void sessionTimeoutIsThirtyDays() {
        assertEquals(
            Duration.ofDays(30),
            serverProperties.getServlet().getSession().getTimeout()
        );
    }

    @Test
    public void sessionCookieIsPersistentForThirtyDays() {
        assertEquals(
            Duration.ofDays(30),
            serverProperties.getServlet().getSession().getCookie().getMaxAge()
        );
    }
}
