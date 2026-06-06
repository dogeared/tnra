package com.afitnerd.tnra.landing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires the optional {@link BasicAuthGateFilter} for password-protecting the
 * landing app during a pre-launch review.
 *
 * Driven by env vars (Spring relaxed binding):
 *   TNRA_BASIC_AUTH_ENABLED   -> tnra.basic-auth.enabled  (default false = public site)
 *   TNRA_BASIC_AUTH_USERNAME  -> tnra.basic-auth.username
 *   TNRA_BASIC_AUTH_PASSWORD  -> tnra.basic-auth.password
 *
 * The filter is only registered when the toggle is on, so the public deploy pays
 * nothing. Enabling without credentials fails fast at startup rather than leaving
 * the site exposed or accidentally accepting blank credentials.
 */
@Configuration
public class BasicAuthConfig {

    static final String REALM = "TNRA Preview";

    @Bean
    @ConditionalOnProperty(name = "tnra.basic-auth.enabled", havingValue = "true")
    FilterRegistrationBean<BasicAuthGateFilter> basicAuthGate(
            @Value("${tnra.basic-auth.username:}") String username,
            @Value("${tnra.basic-auth.password:}") String password) {

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "tnra.basic-auth.enabled=true but no credentials are set. " +
                "Provide TNRA_BASIC_AUTH_USERNAME and TNRA_BASIC_AUTH_PASSWORD.");
        }

        FilterRegistrationBean<BasicAuthGateFilter> registration =
            new FilterRegistrationBean<>(new BasicAuthGateFilter(username, password, REALM));
        // Ahead of Spring Security so the gate covers every request, including
        // Vaadin's internal endpoints and the push handshake.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
