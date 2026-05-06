package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppNotificationTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void successCreatesNotificationWithoutError() {
        assertDoesNotThrow(() -> AppNotification.success("Test success"));
    }

    @Test
    void errorCreatesNotificationWithoutError() {
        assertDoesNotThrow(() -> AppNotification.error("Test error"));
    }

    @Test
    void infoCreatesNotificationWithoutError() {
        assertDoesNotThrow(() -> AppNotification.info("Test info"));
    }
}
