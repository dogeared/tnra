package com.afitnerd.tnra.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringSecurityConfigTest {

    @Test
    void extractsKeycloakRealmAccessRoles() {
        SpringSecurityConfig config = new SpringSecurityConfig();
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
        SpringSecurityConfig config = new SpringSecurityConfig();
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
        SpringSecurityConfig config = new SpringSecurityConfig();
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
}
