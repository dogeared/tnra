package com.afitnerd.tnra.landing.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Content Security Policy. The landing site is pure Vaadin served from this origin with system
     * fonts (no Google Fonts / CDNs), so everything is {@code 'self'}. Vaadin's bootstrap injects inline
     * scripts/styles and uses eval, so {@code script-src}/{@code style-src} keep {@code 'unsafe-inline'}
     * (and {@code 'unsafe-eval'} for scripts) — that's the cost of not breaking Vaadin, and is why this
     * earns an A (not A+; A+ needs a nonce-based policy).
     */
    private static final String CSP = String.join("; ",
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data:",
        "font-src 'self'",
        "connect-src 'self'",
        "object-src 'none'",
        "base-uri 'self'",
        "frame-ancestors 'self'",
        "form-action 'self'");

    private static final String PERMISSIONS_POLICY =
        "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=(self)";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Security response headers. X-Content-Type-Options (nosniff) and X-Frame-Options (DENY) are on
        // by default; we add HSTS, Referrer-Policy, Permissions-Policy, and CSP for a securityheaders.com A.
        http.headers(headers -> headers
            // The app sits behind a Cloudflare tunnel, so the origin sees the request as HTTP and Spring's
            // default (secure-requests-only) HSTS never fires. Force it on every response — the client
            // always receives it over HTTPS via Cloudflare; browsers ignore HSTS arriving over HTTP anyway.
            .httpStrictTransportSecurity(hsts -> hsts
                .requestMatcher(AnyRequestMatcher.INSTANCE)
                .includeSubDomains(true)
                .maxAgeInSeconds(63_072_000)) // 2 years
            .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .permissionsPolicyHeader(pp -> pp.policy(PERMISSIONS_POLICY))
            .contentSecurityPolicy(csp -> csp.policyDirectives(CSP)));

        // VaadinSecurityConfigurer reads @AnonymousAllowed on each view to grant
        // anonymous access. LandingView is annotated, so nothing extra is needed.
        // Explicitly calling `authorizeHttpRequests(...).anyRequest()` before
        // VaadinSecurityConfigurer.init() runs trips a "Can't configure
        // requestMatchers after anyRequest" assertion in Vaadin 24.9.11+.
        http.with(VaadinSecurityConfigurer.vaadin(), cfg -> {});
        return http.build();
    }
}
