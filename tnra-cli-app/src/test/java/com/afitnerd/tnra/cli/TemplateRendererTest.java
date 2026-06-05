package com.afitnerd.tnra.cli;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void substitutesPlaceholders() {
        String result = renderer.substitute("Hello {{NAME}}, port {{PORT}}", Map.of(
            "NAME", "test-group",
            "PORT", "8081"
        ));
        assertEquals("Hello test-group, port 8081", result);
    }

    @Test
    void preservesUnmatchedPlaceholders() {
        String result = renderer.substitute("{{KNOWN}} and {{UNKNOWN}}", Map.of("KNOWN", "yes"));
        assertEquals("yes and {{UNKNOWN}}", result);
    }

    @Test
    void handlesMultilineTemplate() {
        String template = "line1: {{A}}\nline2: {{B}}\nline3: {{A}}";
        String result = renderer.substitute(template, Map.of("A", "alpha", "B", "beta"));
        assertEquals("line1: alpha\nline2: beta\nline3: alpha", result);
    }

    @Test
    void loadsAndRendersClasspathTemplate() throws Exception {
        String result = renderer.render("init-db.sql.tmpl", Map.of(
            "GROUP_NAME", "test",
            "DB_NAME", "tnra_test",
            "DB_USER", "tnra_test",
            "DB_PASSWORD", "secret123",
            "DATE", "2026-04-02"
        ));
        assertTrue(result.contains("CREATE DATABASE IF NOT EXISTS `tnra_test`"));
        assertTrue(result.contains("CREATE USER IF NOT EXISTS 'tnra_test'"));
        assertTrue(result.contains("secret123"));
    }

    @Test
    void missingTemplateThrows() {
        assertThrows(Exception.class,
            () -> renderer.render("nonexistent.tmpl", Map.of()));
    }
}
