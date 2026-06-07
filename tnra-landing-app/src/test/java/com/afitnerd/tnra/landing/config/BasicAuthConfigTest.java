package com.afitnerd.tnra.landing.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthConfigTest {

    private final BasicAuthConfig config = new BasicAuthConfig();

    @Test
    void validCredentialsRegisterGateAheadOfSecurity() {
        FilterRegistrationBean<BasicAuthGateFilter> registration = config.basicAuthGate("admin", "s3cret");

        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
        assertInstanceOf(BasicAuthGateFilter.class, registration.getFilter());
        assertTrue(registration.getFilter().isAuthorized(
            "Basic " + java.util.Base64.getEncoder().encodeToString("admin:s3cret".getBytes())));
    }

    @Test
    void enablingWithBlankUsernameFailsFast() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> config.basicAuthGate("  ", "s3cret"));
        assertTrue(ex.getMessage().contains("TNRA_BASIC_AUTH_USERNAME"));
    }

    @Test
    void enablingWithBlankPasswordFailsFast() {
        assertThrows(IllegalStateException.class, () -> config.basicAuthGate("admin", ""));
    }

    @Test
    void enablingWithNullCredentialsFailsFast() {
        assertThrows(IllegalStateException.class, () -> config.basicAuthGate(null, null));
    }
}
