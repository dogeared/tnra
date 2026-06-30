package com.afitnerd.tnra.landing.seo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricingSeoListenerTest {

    private final PricingSeoListener listener = new PricingSeoListener();

    private IndexHtmlResponse responseForPath(String pathInfo) {
        Document doc = Jsoup.parse("<html><head></head><body><div id=\"outlet\"></div></body></html>");
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getPathInfo()).thenReturn(pathInfo);
        IndexHtmlResponse response = mock(IndexHtmlResponse.class);
        when(response.getDocument()).thenReturn(doc);
        when(response.getVaadinRequest()).thenReturn(request);
        return response;
    }

    @Test
    void serviceInit_registersIndexHtmlListener() {
        ServiceInitEvent event = mock(ServiceInitEvent.class);
        listener.serviceInit(event);
        verify(event).addIndexHtmlRequestListener(any());
    }

    @Test
    void pricingPath_injectsJsonLdAndNoscriptWithPricesTrialAndTax() {
        IndexHtmlResponse response = responseForPath("/pricing");

        listener.injectPricingSeo(response);

        String head = response.getDocument().head().html();
        assertTrue(head.contains("application/ld+json"), "JSON-LD script should be present");
        assertTrue(head.contains("\"7.00\""), "monthly price");
        assertTrue(head.contains("\"60.00\""), "yearly price");
        assertTrue(head.contains("60-day free trial"), "trial disclosure");
        assertTrue(head.contains("Taxes calculated at checkout"), "tax note");

        String body = response.getDocument().body().html();
        assertTrue(body.contains("<noscript>"), "noscript fallback should be present");
        assertTrue(body.contains("$7 / month / member"), "human-readable monthly price");
        assertTrue(body.contains("$60 / year / member"), "human-readable yearly price");
    }

    @Test
    void pricingPathWithTrailingSlash_alsoInjects() {
        IndexHtmlResponse response = responseForPath("/pricing/");

        listener.injectPricingSeo(response);

        assertTrue(response.getDocument().head().html().contains("application/ld+json"));
    }

    @Test
    void nonPricingPath_injectsNothing() {
        IndexHtmlResponse response = responseForPath("/about-us");

        listener.injectPricingSeo(response);

        assertFalse(response.getDocument().head().html().contains("application/ld+json"));
        assertFalse(response.getDocument().body().html().contains("<noscript>"));
    }

    @Test
    void nullPathInfo_injectsNothing() {
        IndexHtmlResponse response = responseForPath(null);

        listener.injectPricingSeo(response);

        assertFalse(response.getDocument().head().html().contains("application/ld+json"));
    }
}
