package com.afitnerd.tnra.vaadin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflinePageTest {

    @Test
    void offlinePageContainsDraftAndSyncUi() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/resources/offline.html")) {
            assertNotNull(stream, "offline.html should be packaged as a static resource");

            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("id=\"sync-now\""));
            assertTrue(html.contains("id=\"finish-after-sync\""));
            assertTrue(html.contains("tnra:posts:offline-draft:v1"));
            assertTrue(html.contains("/api/v1/in_progress"));
            assertTrue(html.contains("/api/v1/finish_from_app"));
            assertTrue(html.contains("response.status === 404"));
            assertTrue(html.contains("window.addEventListener(\"focus\""));
            assertTrue(html.contains("document.addEventListener(\"visibilitychange\""));
        }
    }
}
