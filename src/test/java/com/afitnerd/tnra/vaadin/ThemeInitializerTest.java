package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ThemeInitializerTest {

    @Test
    void addsDarkThemeAttributeWhenCookieRequestsIt() {
        ThemeInitializer initializer = new ThemeInitializer();
        ServiceInitEvent event = mock(ServiceInitEvent.class);
        ArgumentCaptor<IndexHtmlRequestListener> listenerCaptor = ArgumentCaptor.forClass(IndexHtmlRequestListener.class);

        initializer.serviceInit(event);
        org.mockito.Mockito.verify(event).addIndexHtmlRequestListener(listenerCaptor.capture());

        IndexHtmlRequestListener listener = listenerCaptor.getValue();
        IndexHtmlResponse response = mock(IndexHtmlResponse.class);
        Document doc = Jsoup.parse("<html><body></body></html>");
        when(response.getDocument()).thenReturn(doc);

        VaadinRequest request = mock(VaadinRequest.class, withSettings().extraInterfaces(HttpServletRequest.class));
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(MainLayout.DARK_MODE_COOKIE, "true")});
        when(response.getVaadinRequest()).thenReturn(request);

        listener.modifyIndexHtmlResponse(response);
        assertEquals("dark", doc.getElementsByTag("html").first().attr("theme"));
    }

    @Test
    void doesNotSetThemeWhenCookieMissingOrFalse() {
        ThemeInitializer initializer = new ThemeInitializer();
        ServiceInitEvent event = mock(ServiceInitEvent.class);
        ArgumentCaptor<IndexHtmlRequestListener> listenerCaptor = ArgumentCaptor.forClass(IndexHtmlRequestListener.class);

        initializer.serviceInit(event);
        org.mockito.Mockito.verify(event).addIndexHtmlRequestListener(listenerCaptor.capture());

        IndexHtmlResponse response = mock(IndexHtmlResponse.class);
        Document doc = Jsoup.parse("<html><body></body></html>");
        when(response.getDocument()).thenReturn(doc);

        VaadinRequest request = mock(VaadinRequest.class, withSettings().extraInterfaces(HttpServletRequest.class));
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        when(servletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(MainLayout.DARK_MODE_COOKIE, "false")});
        when(response.getVaadinRequest()).thenReturn(request);

        listenerCaptor.getValue().modifyIndexHtmlResponse(response);
        assertEquals("", doc.getElementsByTag("html").first().attr("theme"));
    }
}
