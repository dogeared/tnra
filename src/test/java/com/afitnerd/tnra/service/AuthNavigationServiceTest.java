package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthNavigationServiceTest {

    @Test
    void usesConfiguredPathWhenProvided() {
        AuthNavigationService service = new AuthNavigationService("/custom-login", "oidc");

        assertEquals("/custom-login", service.getLoginPath());
    }

    @Test
    void prefixesConfiguredPathWhenMissingLeadingSlash() {
        AuthNavigationService service = new AuthNavigationService("custom-login", "oidc");

        assertEquals("/custom-login", service.getLoginPath());
    }

    @Test
    void fallsBackToRegistrationIdWhenCustomPathMissing() {
        AuthNavigationService service = new AuthNavigationService(" ", "custom");

        assertEquals("/oauth2/authorization/custom", service.getLoginPath());
    }

    @Test
    void fallsBackToOktaWhenValuesAreBlank() {
        AuthNavigationService service = new AuthNavigationService("", " ");

        assertEquals("/oauth2/authorization/okta", service.getLoginPath());
    }

    @Test
    void rejectsProtocolRelativeConfiguredPath() {
        AuthNavigationService service = new AuthNavigationService("//evil.example/login", "oidc");

        assertEquals("/oauth2/authorization/oidc", service.getLoginPath());
    }

    @Test
    void fallsBackToOktaWhenRegistrationIdContainsUnsafeCharacters() {
        AuthNavigationService service = new AuthNavigationService("", "../bad/id");

        assertEquals("/oauth2/authorization/okta", service.getLoginPath());
    }
}
