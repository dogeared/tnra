package com.afitnerd.tnra.vaadin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceWorkerTest {

    @Test
    void serviceWorkerCachesTheOfflinePageAndServesItForOfflineNavigation() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/resources/service-worker.js")) {
            assertNotNull(stream, "service worker must be packaged as a static resource");

            String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(script.contains("const OFFLINE_PAGE = '/offline.html'"));
            assertTrue(script.contains("cache.add(OFFLINE_PAGE)"));
            assertTrue(script.contains("event.request.mode === 'navigate'"));
        }
    }
}
