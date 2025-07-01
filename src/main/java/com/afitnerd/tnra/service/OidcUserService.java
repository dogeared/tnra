package com.afitnerd.tnra.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OidcUserService {

    /**
     * Get the display name from OIDC authentication
     */
    public String getDisplayName(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            
            // Try to get the full name from OIDC claims
            String fullName = oauth2User.getAttribute("name");
            if (fullName != null && !fullName.isEmpty()) {
                return fullName;
            }
            
            // Fallback to given_name + family_name
            String givenName = oauth2User.getAttribute("given_name");
            String familyName = oauth2User.getAttribute("family_name");
            if (givenName != null && familyName != null) {
                return givenName + " " + familyName;
            }
        }
        
        // Fallback to authentication name
        return authentication.getName();
    }

    /**
     * Get the email from OIDC authentication
     */
    public String getEmail(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
            return oauth2User.getAttribute("email");
        }
        return null;
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
} 