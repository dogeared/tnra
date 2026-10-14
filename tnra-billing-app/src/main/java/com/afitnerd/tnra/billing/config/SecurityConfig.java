package com.afitnerd.tnra.billing.config;

import com.afitnerd.tnra.billing.security.GroupTokenAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Security for the headless billing service. Stateless REST + a signature-verified webhook,
 * so no sessions and no CSRF.
 *
 * <pre>
 *   request flow
 *   ────────────
 *   POST /api/billing/webhook   ── permitAll ──► controller verifies the Paddle-Signature HMAC itself
 *   GET  /actuator/health       ── permitAll
 *   /api/v1/**                  ── authenticated ──► per-group bearer-token filter (T7) scopes
 *                                                    each caller to its own group_slug
 *   anyRequest                  ── denied
 * </pre>
 *
 * The webhook is intentionally anonymous at the Spring layer because Paddle cannot send a
 * bearer token; authenticity is established by HMAC-SHA256 over the raw body inside the controller.
 *
 * <p>The service is publicly reachable ({@code billing.<domain>}) even though only machines call it,
 * so it also sends the full security-header set the two Vaadin apps send. Being JSON-only it can be
 * stricter: the CSP allows <em>no</em> sources at all (nothing is ever rendered), and the referrer
 * policy is {@code no-referrer}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Nothing is rendered, so nothing may load; a JSON body accidentally opened in a browser is inert. */
    static final String CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    static final String PERMISSIONS_POLICY =
        "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=()";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, GroupTokenAuthFilter groupTokenAuthFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // X-Content-Type-Options (nosniff) and X-Frame-Options (DENY) are Spring defaults; add HSTS,
            // Referrer-Policy, Permissions-Policy and CSP for an A on securityheaders.com. HSTS is forced
            // on every response because behind the Cloudflare tunnel the origin sees plain HTTP.
            .headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                    .requestMatcher(AnyRequestMatcher.INSTANCE)
                    .includeSubDomains(true)
                    .maxAgeInSeconds(63_072_000)) // 2 years
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                .permissionsPolicyHeader(pp -> pp.policy(PERMISSIONS_POLICY)))
            .addFilterBefore(groupTokenAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/billing/webhook").permitAll()
                .requestMatchers("/api/admin/**").permitAll() // AdminController enforces X-Admin-Token
                .requestMatchers("/actuator/health").permitAll()
                // Permit the error dispatch so a controller's real status (e.g. the webhook's 401 on a
                // bad signature) reaches the caller instead of being masked as 403 by the /error forward.
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
