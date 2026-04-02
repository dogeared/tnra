package com.afitnerd.tnra.vaadin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineSyncScriptTest {

    @Test
    void postViewScriptQueuesFinishActionWhenOffline() throws Exception {
        Path scriptPath = Path.of("src/main/frontend/tnra-offline-sync.js");
        String script = Files.readString(scriptPath, StandardCharsets.UTF_8);

        assertTrue(script.contains("finish-post-button"));
        assertTrue(script.contains("start-new-post-button"));
        assertTrue(script.contains("tnra-offline-finish-queued"));
        assertTrue(script.contains("tnra-offline-start-redirected"));
        assertTrue(script.contains("finishAfterSync: true"));
        assertTrue(script.contains("window.location.assign(\"/offline.html\")"));
        assertTrue(script.contains("event.stopImmediatePropagation()"));
        assertTrue(script.contains("window.addEventListener(\"focus\""));
        assertTrue(script.contains("document.addEventListener(\"visibilitychange\""));
        assertTrue(script.contains("window.history.pushState = function (...args)"));
        assertTrue(script.contains("window.history.replaceState = function (...args)"));
        assertTrue(script.contains("window.addEventListener(\"tnra-locationchange\""));
        assertTrue(script.contains("ensurePostRouteFeatures();"));
        assertFalse(script.contains("if (!window.location.pathname.startsWith(\"/posts\")) {"));
    }
}
