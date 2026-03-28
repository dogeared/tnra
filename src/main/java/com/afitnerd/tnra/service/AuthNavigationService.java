package com.afitnerd.tnra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthNavigationService {

    private final String loginPath;

    public AuthNavigationService(
        @Value("${tnra.auth.login-path:}") String configuredLoginPath,
        @Value("${tnra.auth.login-registration-id:keycloak}") String registrationId
    ) {
        this.loginPath = resolveLoginPath(configuredLoginPath, registrationId);
    }

    public String getLoginPath() {
        return loginPath;
    }

    static String resolveLoginPath(String configuredLoginPath, String registrationId) {
        if (StringUtils.hasText(configuredLoginPath)) {
            String path = configuredLoginPath.trim();
            return path.startsWith("/") ? path : "/" + path;
        }

        String safeRegistrationId = StringUtils.hasText(registrationId) ? registrationId.trim() : "keycloak";
        return "/oauth2/authorization/" + safeRegistrationId;
    }
}
