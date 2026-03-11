package com.afitnerd.tnra.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringSecurityConfigTest {

    @Test
    void logoutSuccessHandlerRedirectsHome() {
        SpringSecurityConfig config = new SpringSecurityConfig();

        LogoutSuccessHandler handler = config.logoutSuccessHandler();

        assertInstanceOf(SimpleUrlLogoutSuccessHandler.class, handler);
        assertEquals("/", ReflectionTestUtils.getField(handler, "defaultTargetUrl"));
    }

    @Test
    void grantedAuthoritiesMapperPreservesExistingAuthoritiesAndExtractsGroups() {
        SpringSecurityConfig config = new SpringSecurityConfig();
        GrantedAuthoritiesMapper mapper = config.grantedAuthoritiesMapper();
        OidcIdToken token = new OidcIdToken(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of("groups", List.of("admin", "ROLE_REPORTS"))
        );
        OidcUserAuthority oidcAuthority = new OidcUserAuthority(token);

        Set<String> mapped = mapper.mapAuthorities(Set.of(new SimpleGrantedAuthority("ROLE_USER"), oidcAuthority))
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(mapped.contains("ROLE_USER"));
        assertTrue(mapped.contains("ROLE_ADMIN"));
        assertTrue(mapped.contains("ROLE_REPORTS"));
    }

    @Test
    void grantedAuthoritiesMapperFallsBackToRolesAndCommaSeparatedAuthorities() {
        SpringSecurityConfig config = new SpringSecurityConfig();
        GrantedAuthoritiesMapper mapper = config.grantedAuthoritiesMapper();

        OAuth2UserAuthority rolesAuthority = new OAuth2UserAuthority(Map.of("roles", List.of("manager")));
        OAuth2UserAuthority authoritiesAuthority = new OAuth2UserAuthority(Map.of("authorities", "ops, qa"));

        Set<String> mapped = mapper.mapAuthorities(Set.of(rolesAuthority, authoritiesAuthority))
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(mapped.contains("ROLE_MANAGER"));
        assertTrue(mapped.contains("ROLE_OPS"));
        assertTrue(mapped.contains("ROLE_QA"));
    }
}
