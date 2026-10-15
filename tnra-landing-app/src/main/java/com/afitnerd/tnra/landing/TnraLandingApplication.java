package com.afitnerd.tnra.landing;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.server.AppShellSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

// Vaadin 25 applies no theme unless asked; the landing site's styles layer on top of Lumo.
@StyleSheet(Lumo.STYLESHEET)
@SpringBootApplication
public class TnraLandingApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(TnraLandingApplication.class, args);
    }

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.setPageTitle("TNRA — Accountability for Groups That Mean It");

        settings.addMetaTag("description",
            "TNRA is a structured accountability app for recovery groups, faith communities, " +
            "and professional accountability circles.");
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1");

        // Same icon set as the group app (copied from tnra-app's META-INF/resources/icons), so a tab,
        // bookmark or iOS home-screen shortcut for tnra.app looks like the app itself. No manifest:
        // the marketing site is not an installable app.
        settings.addFavIcon("icon", "/icons/icon-192.png", "192x192");
        settings.addLink("/icons/apple-touch-icon.png", Map.of("rel", "apple-touch-icon", "sizes", "180x180"));
        settings.addMetaTag("theme-color", "#2D6A4F");
    }
}
