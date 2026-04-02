package com.afitnerd.tnra.cli;

import java.util.Set;
import java.util.regex.Pattern;

public class GroupNameValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{2,30}$");
    private static final Set<String> RESERVED = Set.of(
        "www", "api", "admin", "mail", "smtp", "ftp", "localhost", "keycloak", "mysql", "nginx"
    );

    public static String validate(String name) {
        if (name == null || name.isBlank()) {
            return "Group name cannot be empty";
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return "Group name must be 3-31 lowercase letters, digits, or hyphens, starting with a letter";
        }
        if (name.startsWith("-") || name.endsWith("-")) {
            return "Group name cannot start or end with a hyphen";
        }
        if (RESERVED.contains(name)) {
            return "'" + name + "' is a reserved name";
        }
        return null; // valid
    }
}
