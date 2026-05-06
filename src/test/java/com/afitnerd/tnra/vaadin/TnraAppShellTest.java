package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.server.PWA;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TnraAppShellTest {

    @Test
    void pwaAnnotationConfigurationIsPresent() {
        PWA pwa = TnraAppShell.class.getAnnotation(PWA.class);

        assertNotNull(pwa);
        assertEquals("TNRA", pwa.name());
        assertEquals("TNRA", pwa.shortName());
        assertEquals("images/pwa-icon.svg", pwa.iconPath());
        assertEquals("offline.html", pwa.offlinePath());
        assertArrayEquals(new String[]{"images/pwa-icon.svg"}, pwa.offlineResources());
    }
}
