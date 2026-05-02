package com.afitnerd.tnra.landing;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
    }
}
