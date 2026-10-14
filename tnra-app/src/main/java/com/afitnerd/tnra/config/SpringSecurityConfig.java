package com.afitnerd.tnra.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    /**
     * Content Security Policy, minus {@code form-action} (see {@link #contentSecurityPolicy(String)}).
     * The group app is pure Vaadin served from its own origin: system fonts, profile images from
     * {@code /uploads}, CSV export via a same-origin stream resource, no push/websocket, no iframes.
     * {@code font-src} must allow {@code data:}: Lumo ships its icon font ({@code lumo-icons}) as an
     * embedded data URL, and without it the browser blocks the font and icon glyphs vanish.
     * Paddle never runs here — checkout is a full-page navigation to the landing site's {@code /pay},
     * and the customer portal is a navigation to Paddle — so nothing external is allowed. Vaadin's
     * bootstrap injects inline scripts/styles and uses eval, hence {@code 'unsafe-inline'} /
     * {@code 'unsafe-eval'}; that caps securityheaders.com at an A (A+ needs a nonce-based policy).
     */
    static final String CSP_BASE = String.join("; ",
        "default-src 'self'",
        "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
        "style-src 'self' 'unsafe-inline'",
        "img-src 'self' data:",
        "font-src 'self' data:",
        "connect-src 'self'",
        "object-src 'none'",
        "base-uri 'self'",
        "frame-ancestors 'none'");

    static final String PERMISSIONS_POLICY =
        "camera=(), microphone=(), geolocation=(), payment=(), usb=(), fullscreen=(self)";

    @Value("${tnra.auth.login-registration-id:keycloak}")
    private String loginRegistrationId;

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SpringSecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.oauth2LoginPage("/oauth2/authorization/" + loginRegistrationId);
            // Vaadin 25 defaults every non-Vaadin request to denyAll (24 used authenticated). Our own
            // endpoints — /uploads/** (profile images) — must stay reachable to a signed-in member.
            configurer.anyRequest(AuthorizedUrl::authenticated);
        });

        http
            // Security response headers. X-Content-Type-Options (nosniff) and X-Frame-Options (DENY) are
            // Spring defaults; HSTS, Referrer-Policy, Permissions-Policy and CSP complete the set that
            // securityheaders.com grades (same policy as tnra-landing, plus the Keycloak form-action).
            .headers(headers -> headers
                // Behind the Cloudflare tunnel the origin may see plain HTTP, and Spring's default HSTS
                // writer only fires on secure requests. Force it on every response: the client always
                // receives it over HTTPS, and browsers ignore HSTS that arrives over HTTP anyway.
                .httpStrictTransportSecurity(hsts -> hsts
                    .requestMatcher(AnyRequestMatcher.INSTANCE)
                    .includeSubDomains(true)
                    .maxAgeInSeconds(63_072_000)) // 2 years
                .referrerPolicy(ref -> ref.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    contentSecurityPolicy(keycloakOrigin(clientRegistrationRepository, loginRegistrationId))))
                .permissionsPolicyHeader(pp -> pp.policy(PERMISSIONS_POLICY))
                // Vaadin 25 commits page responses in a way Spring's lazy header writer never observes, so
                // without this every view shipped with NO security headers. Write them at filter entry.
                .withObjectPostProcessor(eagerHeaderWriting()))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(
                    userInfo -> userInfo.userAuthoritiesMapper(grantedAuthoritiesMapper())
                )
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"));

        return http.build();
    }

    /**
     * The full CSP. {@code form-action} must list the Keycloak origin: logout is a form POST to
     * {@code /logout} whose success handler redirects to Keycloak's end-session endpoint, and Chrome
     * applies {@code form-action} to the redirect chain of a form submission — with {@code 'self'} alone
     * the browser would block that redirect and the member would stay signed in at Keycloak. Login is a
     * plain navigation (GET) so it is unaffected either way.
     *
     * @param keycloakOrigin scheme://host[:port] of the Keycloak server, or {@code null} to allow only self
     */
    static String contentSecurityPolicy(String keycloakOrigin) {
        String formAction = keycloakOrigin == null ? "form-action 'self'" : "form-action 'self' " + keycloakOrigin;
        return CSP_BASE + "; " + formAction;
    }

    /**
     * Resolves Keycloak's public origin from the login registration's authorization URI — the one URI
     * the browser is actually sent to (token/userinfo may point at the internal Docker hostname).
     * Returns {@code null} when the registration is missing or the URI is unusable, so a misconfiguration
     * degrades to a self-only policy rather than failing startup.
     */
    static String keycloakOrigin(ClientRegistrationRepository repository, String registrationId) {
        try {
            ClientRegistration registration = repository.findByRegistrationId(registrationId);
            if (registration == null) {
                return null;
            }
            String authorizationUri = registration.getProviderDetails().getAuthorizationUri();
            if (authorizationUri == null) {
                return null;
            }
            URI uri = URI.create(authorizationUri);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
        } catch (RuntimeException e) {
            return null;
        }
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

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }

    @Bean
    public GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    extractKeycloakRoles(oidcUserAuthority.getIdToken().getClaims(), mappedAuthorities);
                }
            });

            return mappedAuthorities;
        };
    }

    private void extractKeycloakRoles(Map<String, Object> claims, Set<GrantedAuthority> mappedAuthorities) {
        if (claims.get("realm_access") instanceof Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof Collection<?> roles) {
            roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                .forEach(mappedAuthorities::add);
        }
    }
}
