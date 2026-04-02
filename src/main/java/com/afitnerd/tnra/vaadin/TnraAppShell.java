package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

@PWA(
    name = "TNRA",
    shortName = "TNRA",
    description = "TNRA mobile app with installable offline support",
    iconPath = "images/pwa-icon.svg",
    offlinePath = "offline.html",
    offlineResources = {"images/pwa-icon.svg"}
)
@Viewport("width=device-width, initial-scale=1, viewport-fit=cover")
public class TnraAppShell implements AppShellConfigurator {
}
