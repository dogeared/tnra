package com.afitnerd.tnra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
public class AuthNavigationService {

    private static final String DEFAULT_REGISTRATION_ID = "okta";
    private static final Pattern REGISTRATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");

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
            if (isSafeRelativePath(path)) {
                return path.startsWith("/") ? path : "/" + path;
            }
        }

        String safeRegistrationId = sanitizeRegistrationId(registrationId);
        return "/oauth2/authorization/" + safeRegistrationId;
    }

    private static boolean isSafeRelativePath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        if (path.startsWith("//")) {
            return false;
        }
        if (path.contains("://")) {
            return false;
        }
        return !path.contains("\\");
    }

    private static String sanitizeRegistrationId(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            return DEFAULT_REGISTRATION_ID;
        }

        String candidate = registrationId.trim();
        if (!REGISTRATION_ID_PATTERN.matcher(candidate).matches()) {
            return DEFAULT_REGISTRATION_ID;
        }
        return candidate;
    }
}
