package com.afitnerd.tnra.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression coverage for the 30-day "stay logged in" behavior.
 *
 * The bug: only server.servlet.session.timeout was set, so JSESSIONID had no
 * max-age and was a browser-session cookie — dropped when the browser closed,
 * logging users out long before the 30-day server session (and Keycloak SSO
 * session) expired. The cookie max-age must match the session timeout.
 *
 * This reads the shipped src/main/resources/application.properties directly
 * rather than asserting against a Spring ServerProperties bean: in tests the
 * main properties file is shadowed by src/test/resources/application.properties,
 * and the local-dev application.yml that also carries these values is gitignored,
 * so a context-based assertion passes locally but sees Spring defaults in CI.
 * Reading the file guards the artifact that actually deploys.
 */
public class SessionConfigTest {

    private Properties shippedApplicationProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            props.load(in);
        }
        return props;
    }

    @Test
    public void sessionTimeoutIsThirtyDays() throws Exception {
        assertEquals("30d", shippedApplicationProperties().getProperty("server.servlet.session.timeout"));
    }

    @Test
    public void sessionCookieIsPersistentForThirtyDays() throws Exception {
        assertEquals("30d", shippedApplicationProperties().getProperty("server.servlet.session.cookie.max-age"));
    }
}
