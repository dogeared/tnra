package com.afitnerd.tnra.vaadin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineSyncScriptTest {

    @Test
    void postViewScriptQueuesFinishActionWhenOffline() throws Exception {
        Path scriptPath = Path.of("src/main/frontend/tnra-offline-sync.js");
        String script = Files.readString(scriptPath, StandardCharsets.UTF_8);

        assertTrue(script.contains("finish-post-button"));
        assertTrue(script.contains("tnra-offline-finish-queued"));
        assertTrue(script.contains("finishAfterSync: true"));
        assertTrue(script.contains("event.stopImmediatePropagation()"));
    }
}
