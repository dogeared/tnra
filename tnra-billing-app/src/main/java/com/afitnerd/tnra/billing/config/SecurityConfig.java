package com.afitnerd.tnra.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for the headless billing service. Stateless REST + a signature-verified webhook,
 * so no sessions and no CSRF.
 *
 * <pre>
 *   request flow
 *   ────────────
 *   POST /api/billing/webhook   ── permitAll ──► controller verifies Lemon Squeezy HMAC itself
 *   GET  /actuator/health       ── permitAll
 *   /api/v1/**                  ── authenticated ──► per-group bearer-token filter (T7) scopes
 *                                                    each caller to its own group_slug
 *   anyRequest                  ── denied
 * </pre>
 *
 * The webhook is intentionally anonymous at the Spring layer because Lemon Squeezy cannot send a
 * bearer token; authenticity is established by HMAC-SHA256 over the raw body inside the controller.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/billing/webhook").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
