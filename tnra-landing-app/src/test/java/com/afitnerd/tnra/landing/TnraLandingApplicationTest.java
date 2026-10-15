package com.afitnerd.tnra.landing;

import com.vaadin.flow.server.AppShellSettings;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The landing site must present the same icon as the group app (same files, same link tags), so a
 * tab or home-screen shortcut for tnra.app is recognisably TNRA. Guards against an icon being
 * renamed/removed or the shell losing the links.
 */
class TnraLandingApplicationTest {

    @Test
    void shell_declaresTheSameFaviconAndTouchIconAsTheGroupApp() {
        AppShellSettings settings = mock(AppShellSettings.class);

        new TnraLandingApplication().configurePage(settings);

        verify(settings).addFavIcon("icon", "/icons/icon-192.png", "192x192");
        verify(settings).addLink("/icons/apple-touch-icon.png", Map.of("rel", "apple-touch-icon", "sizes", "180x180"));
        verify(settings).addMetaTag("theme-color", "#2D6A4F");
        verify(settings).setPageTitle("TNRA — Accountability for Groups That Mean It");
        verify(settings).addMetaTag(eq("description"), org.mockito.ArgumentMatchers.contains("accountability"));
    }

    @Test
    void bothIconFilesShipInTheJar() throws Exception {
        for (String icon : new String[] {"/icons/icon-192.png", "/icons/apple-touch-icon.png"}) {
            try (InputStream in = getClass().getResourceAsStream("/META-INF/resources" + icon)) {
                assertNotNull(in, icon + " must be on the classpath");
            }
        }
    }
}
