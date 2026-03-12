package com.afitnerd.tnra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthNavigationService {

    private static final String DEFAULT_REGISTRATION_ID = "okta";
    private static final String OAUTH_LOGIN_PREFIX = "/oauth2/authorization/";

    private final String loginPath;

    public AuthNavigationService(
        @Value("${tnra.auth.login-path:}") String configuredLoginPath,
        @Value("${tnra.auth.login-registration-id:okta}") String registrationId
    ) {
        this.loginPath = resolveLoginPath(configuredLoginPath, registrationId);
    }

    public String getLoginPath() {
        return loginPath;
    }

    static String resolveLoginPath(String configuredLoginPath, String registrationId) {
        if (StringUtils.hasText(configuredLoginPath)) {
            String path = configuredLoginPath.trim();
            String normalizedPath = path.startsWith("/") ? path : "/" + path;

            // Prevent protocol-relative redirects like "//evil.example".
            if (normalizedPath.startsWith("//")) {
                return OAUTH_LOGIN_PREFIX + resolveRegistrationId(registrationId);
            }
            return normalizedPath;
        }

        return OAUTH_LOGIN_PREFIX + resolveRegistrationId(registrationId);
    }

    private static String resolveRegistrationId(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return DEFAULT_REGISTRATION_ID;
        }

        String trimmedRegistrationId = registrationId.trim();
        if (!trimmedRegistrationId.matches("[A-Za-z0-9_-]+")) {
            return DEFAULT_REGISTRATION_ID;
        }

        return trimmedRegistrationId;
    }
}
