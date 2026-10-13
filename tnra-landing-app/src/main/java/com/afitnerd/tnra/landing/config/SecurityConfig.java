package com.afitnerd.tnra.landing.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.config.ObjectPostProcessor;
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
    static final String CSP = String.join("; ",
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

    /**
     * CSP for the {@code /pay} checkout page only. Paddle.js loads from {@code cdn.paddle.com}, renders
     * the checkout in an iframe from {@code buy.paddle.com} ({@code sandbox-buy.paddle.com} in sandbox),
     * and talks to Paddle's APIs from the page. Everything else stays as tight as the site-wide policy;
     * scoping the exceptions to this one path keeps the apex scan at an A.
     */
    static final String PAY_CSP = String.join("; ",
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.paddle.com",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data: https://*.paddle.com",
        "font-src 'self'",
        "connect-src 'self' https://*.paddle.com",
        "frame-src https://buy.paddle.com https://sandbox-buy.paddle.com",
        "object-src 'none'",
        "base-uri 'self'",
        "frame-ancestors 'self'",
        "form-action 'self'");

    static final String PERMISSIONS_POLICY =
        "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=(self)";

    /** On {@code /pay} the Paddle iframe may use the Payment Request API (Apple Pay / Google Pay). */
    static final String PAY_PERMISSIONS_POLICY =
        "camera=(), microphone=(), geolocation=(), "
        + "payment=(self \"https://buy.paddle.com\" \"https://sandbox-buy.paddle.com\"), "
        + "usb=(), fullscreen=(self)";

    static final String PAY_PATH = "/pay";

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
            // CSP + Permissions-Policy depend on the path (/pay needs Paddle), so they're one writer
            // rather than the fixed-string .contentSecurityPolicy()/.permissionsPolicyHeader() helpers.
            .addHeaderWriter(pathAwareHeaders())
            // Spring Security writes headers lazily, when the response commits. Vaadin 25's page handler
            // commits the response in a way the lazy writer never observes, so Vaadin routes shipped with
            // NO security headers (verified on the production jar: /api/* had them, /pricing did not).
            // Writing eagerly — at filter entry — makes them unconditional.
            .withObjectPostProcessor(eagerHeaderWriting()));

        // VaadinSecurityConfigurer reads @AnonymousAllowed on each view to grant
        // anonymous access. LandingView is annotated, so nothing extra is needed.
        // Explicitly calling `authorizeHttpRequests(...).anyRequest()` before
        // VaadinSecurityConfigurer.init() runs trips a "Can't configure
        // requestMatchers after anyRequest" assertion in Vaadin 24.9.11+.
        http.with(VaadinSecurityConfigurer.vaadin(), cfg -> {});
        return http.build();
    }

    /** See the note in {@link #filterChain}: headers must be written before Vaadin commits the response. */
    static ObjectPostProcessor<HeaderWriterFilter> eagerHeaderWriting() {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends HeaderWriterFilter> O postProcess(O filter) {
                filter.setShouldWriteHeadersEagerly(true);
                return filter;
            }
        };
    }

    /**
     * Writes Content-Security-Policy and Permissions-Policy, choosing the Paddle-enabled variants for the
     * {@code /pay} page. Only the initial document request for {@code /pay} matters: Paddle sends the
     * member here with a full page load, so the headers on that response govern Paddle.js and its iframe.
     */
    static HeaderWriter pathAwareHeaders() {
        return (HttpServletRequest request, HttpServletResponse response) -> {
            boolean pay = isPayPath(request);
            response.setHeader("Content-Security-Policy", pay ? PAY_CSP : CSP);
            response.setHeader("Permissions-Policy", pay ? PAY_PERMISSIONS_POLICY : PERMISSIONS_POLICY);
        };
    }

    static boolean isPayPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path == null) {
            return false;
        }
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return PAY_PATH.equals(path) || (PAY_PATH + "/").equals(path);
    }
}
