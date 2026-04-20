package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Centralized notification helper. All app notifications use MIDDLE position
 * to avoid being hidden behind the nav drawer.
 */
public final class AppNotification {

    private static final int SUCCESS_DURATION = 3000;
    private static final int ERROR_DURATION = 5000;
    private static final Notification.Position POSITION = Notification.Position.MIDDLE;

    private AppNotification() {}

    public static void success(String message) {
        Notification notification = new Notification();
        notification.setText(message);
        notification.setDuration(SUCCESS_DURATION);
        notification.setPosition(POSITION);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static void error(String message) {
        Notification notification = new Notification();
        notification.setText(message);
        notification.setDuration(ERROR_DURATION);
        notification.setPosition(POSITION);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    public static void info(String message) {
        Notification notification = new Notification();
        notification.setText(message);
        notification.setDuration(SUCCESS_DURATION);
        notification.setPosition(POSITION);
        notification.open();
    }
}
