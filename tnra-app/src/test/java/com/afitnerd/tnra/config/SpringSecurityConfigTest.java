package com.afitnerd.tnra.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringSecurityConfigTest {

    private final ClientRegistrationRepository clientRegistrationRepository =
        mock(ClientRegistrationRepository.class);

    @Test
    void extractsKeycloakRealmAccessRoles() {
        SpringSecurityConfig config = new SpringSecurityConfig(clientRegistrationRepository);
        GrantedAuthoritiesMapper mapper = config.grantedAuthoritiesMapper();
        OidcIdToken token = new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("realm_access", Map.of("roles", List.of("admin", "member")))
        );
        OidcUserAuthority oidcAuthority = new OidcUserAuthority(token);

        Set<String> mapped = mapper.mapAuthorities(Set.of(oidcAuthority))
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        assertTrue(mapped.contains("ROLE_ADMIN"));
        assertTrue(mapped.contains("ROLE_MEMBER"));
    }

    @Test
    void preservesExistingAuthorities() {
        SpringSecurityConfig config = new SpringSecurityConfig(clientRegistrationRepository);
        GrantedAuthoritiesMapper mapper = config.grantedAuthoritiesMapper();
        OidcIdToken token = new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("realm_access", Map.of("roles", List.of("admin")))
        );
        OidcUserAuthority oidcAuthority = new OidcUserAuthority(token);

        Set<String> mapped = mapper.mapAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_USER"), oidcAuthority))
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        assertTrue(mapped.contains("ROLE_USER"));
        assertTrue(mapped.contains("ROLE_ADMIN"));
    }

    @Test
    void handlesMissingRealmAccess() {
        SpringSecurityConfig config = new SpringSecurityConfig(clientRegistrationRepository);
        GrantedAuthoritiesMapper mapper = config.grantedAuthoritiesMapper();
        OidcIdToken token = new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("sub", "user123")
        );
        OidcUserAuthority oidcAuthority = new OidcUserAuthority(token);

        Set<String> mapped = mapper.mapAuthorities(Set.of(oidcAuthority))
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        assertTrue(mapped.stream().anyMatch(a -> a.equals("OIDC_USER")));
        assertTrue(mapped.stream().noneMatch(a -> a.equals("ROLE_ADMIN")));
    }

    // ---- security response headers (securityheaders.com A) ----

    private static ClientRegistration registrationWithAuthorizationUri(String authorizationUri) {
        return ClientRegistration.withRegistrationId("keycloak")
            .clientId("the-og-app")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .authorizationUri(authorizationUri)
            // token/userinfo deliberately point at the Docker-internal hostname, as production does;
            // the browser-facing origin must come from the authorization URI alone.
            .tokenUri("http://keycloak:8080/realms/the-og/protocol/openid-connect/token")
            .build();
    }

    @Test
    void keycloakOrigin_isSchemeAndHostOfTheAuthorizationUri() {
        when(clientRegistrationRepository.findByRegistrationId("keycloak"))
            .thenReturn(registrationWithAuthorizationUri(
                "https://auth.tnra.app/realms/the-og/protocol/openid-connect/auth"));

        assertEquals("https://auth.tnra.app",
            SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "keycloak"));
    }

    @Test
    void keycloakOrigin_keepsAnExplicitPort() {
        when(clientRegistrationRepository.findByRegistrationId("keycloak"))
            .thenReturn(registrationWithAuthorizationUri(
                "http://localhost:8180/realms/tnra/protocol/openid-connect/auth"));

        assertEquals("http://localhost:8180",
            SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "keycloak"));
    }

    @Test
    void keycloakOrigin_isNull_whenRegistrationMissing() {
        when(clientRegistrationRepository.findByRegistrationId("keycloak")).thenReturn(null);

        assertNull(SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "keycloak"));
    }

    @Test
    void keycloakOrigin_isNull_whenRegistrationHasNoAuthorizationUri() {
        ClientRegistration clientCredentials = ClientRegistration.withRegistrationId("keycloak")
            .clientId("svc")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak:8080/token")
            .build();
        when(clientRegistrationRepository.findByRegistrationId("keycloak")).thenReturn(clientCredentials);

        assertNull(SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "keycloak"));
    }

    @Test
    void keycloakOrigin_isNull_whenAuthorizationUriHasNoHost_orIsMalformed_orLookupThrows() {
        when(clientRegistrationRepository.findByRegistrationId("nohost"))
            .thenReturn(registrationWithAuthorizationUri("mailto:someone@example.com"));
        assertNull(SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "nohost"));

        when(clientRegistrationRepository.findByRegistrationId("malformed"))
            .thenReturn(registrationWithAuthorizationUri("http://bad host/auth"));
        assertNull(SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "malformed"));

        when(clientRegistrationRepository.findByRegistrationId("boom"))
            .thenThrow(new IllegalStateException("registry unavailable"));
        assertNull(SpringSecurityConfig.keycloakOrigin(clientRegistrationRepository, "boom"));
    }

    @Test
    void contentSecurityPolicy_allowsLogoutRedirectToKeycloak_viaFormAction() {
        String csp = SpringSecurityConfig.contentSecurityPolicy("https://auth.tnra.app");

        assertTrue(csp.endsWith("form-action 'self' https://auth.tnra.app"));
        assertTrue(csp.startsWith(SpringSecurityConfig.CSP_BASE + "; "));
    }

    @Test
    void contentSecurityPolicy_isSelfOnly_withoutAKeycloakOrigin() {
        String csp = SpringSecurityConfig.contentSecurityPolicy(null);

        assertTrue(csp.endsWith("form-action 'self'"));
        assertFalse(csp.contains("auth."));
    }

    @Test
    void contentSecurityPolicy_keepsVaadinWorking_andAllowsNothingExternal() {
        String csp = SpringSecurityConfig.contentSecurityPolicy("https://auth.tnra.app");

        assertTrue(csp.contains("script-src 'self' 'unsafe-inline' 'unsafe-eval'"));
        assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"));
        assertTrue(csp.contains("img-src 'self' data:"));
        assertTrue(csp.contains("font-src 'self' data:"), "Lumo icon font is an embedded data: URL");
        assertTrue(csp.contains("connect-src 'self'"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("base-uri 'self'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertFalse(csp.contains("paddle"), "Paddle only ever runs on the landing site's /pay page");
        assertFalse(SpringSecurityConfig.CSP_BASE.contains("http"), "nothing external outside form-action");
    }

    @Test
    void permissionsPolicy_disablesSensitiveFeatures() {
        String pp = SpringSecurityConfig.PERMISSIONS_POLICY;

        assertTrue(pp.contains("camera=()"));
        assertTrue(pp.contains("microphone=()"));
        assertTrue(pp.contains("geolocation=()"));
        assertTrue(pp.contains("payment=()"));
        assertTrue(pp.contains("fullscreen=(self)"));
    }

    @Test
    void headersAreWrittenEagerly_soVaadinPageResponsesCarryThem() {
        HeaderWriterFilter filter = new HeaderWriterFilter(List.of((req, res) -> { }));

        HeaderWriterFilter processed = SpringSecurityConfig.eagerHeaderWriting().postProcess(filter);

        assertSame(filter, processed);
        assertTrue((boolean) ReflectionTestUtils.getField(filter, "shouldWriteHeadersEagerly"));
    }
}
