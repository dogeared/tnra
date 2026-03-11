package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.nodes.Element;

/**
 * Reads the dark-mode cookie during the initial page render and sets
 * theme="dark" on the {@code <html>} element before the page is sent
 * to the browser. This prevents a flash of the wrong theme on load
 * — entirely in Java, no inline JavaScript needed.
 */
@SpringComponent
public class ThemeInitializer implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(new DarkModeIndexHtmlListener());
    }

    private static class DarkModeIndexHtmlListener implements IndexHtmlRequestListener {

        @Override
        public void modifyIndexHtmlResponse(IndexHtmlResponse response) {
            if (isDarkModeRequested(response)) {
                Element html = response.getDocument().getElementsByTag("html").first();
                if (html != null) {
                    // Vaadin and Lumo both key off the "theme" attribute
                    html.attr("theme", "dark");
                }
            }
        }

        private boolean isDarkModeRequested(IndexHtmlResponse response) {
            // VaadinServletRequest extends HttpServletRequestWrapper, so it implements HttpServletRequest
            if (response.getVaadinRequest() instanceof HttpServletRequest httpRequest) {
                Cookie[] cookies = httpRequest.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (MainLayout.DARK_MODE_COOKIE.equals(cookie.getName())) {
                            return "true".equals(cookie.getValue());
                        }
                    }
                }
            }
            return false;
        }
    }
}
