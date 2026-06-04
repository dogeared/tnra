package com.afitnerd.tnra.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for "add to home screen" support, which is wired by hand
 * (see {@link com.afitnerd.tnra.TnraApplication}) rather than via @PWA because the
 * @PWA build-frontend step crashes on Vaadin 24.9.9. These assets are static, so
 * this guards against an icon being renamed/removed or the manifest losing the
 * short label that keeps the home-screen icon reading "TNRA".
 */
public class HomeScreenInstallTest {

    private InputStream resource(String path) {
        return getClass().getResourceAsStream("/META-INF/resources" + path);
    }

    @Test
    public void manifestDeclaresTnraStandaloneApp() throws Exception {
        try (InputStream in = resource("/manifest.webmanifest")) {
            assertNotNull(in, "manifest.webmanifest must be on the classpath");
            JsonNode manifest = new ObjectMapper().readTree(in);

            assertEquals("TNRA", manifest.path("short_name").asText(),
                "short_name is the home-screen label on iOS and Android");
            assertEquals("standalone", manifest.path("display").asText());
            assertTrue(manifest.path("icons").isArray() && !manifest.path("icons").isEmpty(),
                "manifest must declare at least one icon");
        }
    }

    @Test
    public void everyManifestIconExists() throws Exception {
        JsonNode manifest;
        try (InputStream in = resource("/manifest.webmanifest")) {
            manifest = new ObjectMapper().readTree(in);
        }
        for (JsonNode icon : manifest.path("icons")) {
            String src = icon.path("src").asText();
            try (InputStream iconStream = resource(src)) {
                assertNotNull(iconStream, "manifest references missing icon: " + src);
            }
        }
    }

    @Test
    public void appleTouchIconExistsForIos() throws Exception {
        // iOS ignores the manifest icons and uses the apple-touch-icon link.
        try (InputStream in = resource("/icons/apple-touch-icon.png")) {
            assertNotNull(in, "apple-touch-icon.png must exist for iOS home-screen install");
        }
    }
}
