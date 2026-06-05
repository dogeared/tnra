package com.afitnerd.tnra.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcUserServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getDisplayNamePrefersFullNameThenFallsBackToGivenAndFamilyName() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getFullName()).thenReturn("Full Name");
        when(oidcUser.getGivenName()).thenReturn("Given");
        when(oidcUser.getFamilyName()).thenReturn("Family");

        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(oidcUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        OidcUserService service = new OidcUserService();

        assertEquals("Full Name", service.getDisplayName());

        when(oidcUser.getFullName()).thenReturn("");
        assertEquals("Given Family", service.getDisplayName());
    }

    @Test
    void getDisplayNameFallsBackToAuthenticationNameWhenNamesMissing() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getFullName()).thenReturn(null);
        when(oidcUser.getGivenName()).thenReturn(null);
        when(oidcUser.getFamilyName()).thenReturn(null);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            oidcUser,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        OidcUserService service = new OidcUserService();

        assertEquals(authentication.getName(), service.getDisplayName());
    }

    @Test
    void getEmailAndAuthenticationChecksUseCurrentPrincipal() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getEmail()).thenReturn("user@example.com");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            oidcUser,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        OidcUserService service = new OidcUserService();

        assertEquals("user@example.com", service.getEmail());
        assertTrue(service.isAuthenticated());
        assertFalse(service.isOidcUser(authentication));
    }

    @Test
    void getAttributeReadsOauth2AuthenticationAttributes() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getAttribute("department")).thenReturn("engineering");
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class, Mockito.RETURNS_DEEP_STUBS);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        OidcUserService service = new OidcUserService();

        assertEquals("engineering", service.getAttribute(authentication, "department"));
        assertTrue(service.isOidcUser(authentication));
        assertNull(service.getAttribute(new TestingAuthenticationToken("user", null), "department"));
    }

    @Test
    void isAuthenticatedRejectsAnonymousOrMissingAuthentication() {
        SecurityContextHolder.clearContext();
        OidcUserService noAuthService = new OidcUserService();
        assertFalse(noAuthService.isAuthenticated());

        TestingAuthenticationToken anonymous = new TestingAuthenticationToken("anonymousUser", null);
        anonymous.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        OidcUserService anonymousService = new OidcUserService();
        assertFalse(anonymousService.isAuthenticated());
    }
}
