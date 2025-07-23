package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

import java.time.ZoneId;
import java.time.ZoneOffset;

@SpringComponent
public class MyServiceInitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            UI ui = uiInitEvent.getUI();
            // Fetch extended client details as soon as the UI is ready
            ui.getPage().retrieveExtendedClientDetails(details -> {
                // Store ExtendedClientDetails in the session
                ui.getSession().setAttribute(ExtendedClientDetails.class, details);
            });
        });
    }
}