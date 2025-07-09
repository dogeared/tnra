package com.afitnerd.tnra.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class OidcUserService {

    private final Authentication authentication;
    private final OidcUser oidcUser;

    public OidcUserService() {
        this.authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            this.oidcUser = (OidcUser) authentication.getPrincipal();
        } else {
            this.oidcUser = null;
        }
    }

    /**
     * Get the display name from OIDC authentication
     */
    public String getDisplayName() {
        String fullName = oidcUser.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }

        String givenName = oidcUser.getGivenName();
        String familyName = oidcUser.getFamilyName();

        if (givenName != null && familyName != null) {
            return givenName + " " + familyName;
        }

        return authentication.getName();
    }

    /**
     * Get the email from OIDC authentication
     */
    public String getEmail() {
        return oidcUser.getEmail();
    }

    /**
     * Get any attribute from OIDC authentication
     */
    public <T> T getAttribute(Authentication authentication, String attributeName) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            return oauth2User.getAttribute(attributeName);
        }
        return null;
    }

    /**
     * Check if the authentication is OIDC-based
     */
    public boolean isOidcUser(Authentication authentication) {
        return authentication instanceof OAuth2AuthenticationToken;
    }

    public boolean isAuthenticated() {
        return 
            authentication != null && 
            authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getName());
    }
} 