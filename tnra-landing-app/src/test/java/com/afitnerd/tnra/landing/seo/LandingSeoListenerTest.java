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

class LandingSeoListenerTest {

    private final LandingSeoListener listener = new LandingSeoListener();

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
    void everyPage_getsCrawlableNavLinkingToPricing() {
        IndexHtmlResponse response = responseForPath("/about-us");

        listener.injectSeo(response);

        String body = response.getDocument().body().html();
        assertTrue(body.contains("<noscript>"), "noscript nav should be present");
        assertTrue(body.contains("href=\"/pricing\""), "nav must link to /pricing");
        assertTrue(body.contains("href=\"/fine-print\""), "nav must link to the legal pages");
    }

    @Test
    void nonPricingPage_getsNavButNoPricingData() {
        IndexHtmlResponse response = responseForPath("/about-us");

        listener.injectSeo(response);

        assertTrue(response.getDocument().body().html().contains("href=\"/pricing\""), "nav present");
        assertFalse(response.getDocument().head().html().contains("application/ld+json"),
            "no pricing JSON-LD off the pricing page");
        assertFalse(response.getDocument().body().html().contains("pricing-fallback"),
            "no pricing summary off the pricing page");
    }

    @Test
    void pricingPage_getsNavAndJsonLdAndSummaryWithPricesTrialAndTax() {
        IndexHtmlResponse response = responseForPath("/pricing");

        listener.injectSeo(response);

        String head = response.getDocument().head().html();
        assertTrue(head.contains("application/ld+json"), "JSON-LD script should be present");
        assertTrue(head.contains("\"7.00\""), "monthly price");
        assertTrue(head.contains("\"60.00\""), "yearly price");
        assertTrue(head.contains("60-day free trial"), "trial disclosure");
        assertTrue(head.contains("Taxes calculated at checkout"), "tax note");

        String body = response.getDocument().body().html();
        assertTrue(body.contains("href=\"/pricing\""), "nav present on the pricing page too");
        assertTrue(body.contains("pricing-fallback"), "human-readable pricing summary");
        assertTrue(body.contains("$7 / month / member"), "monthly price in summary");
        assertTrue(body.contains("$60 / year / member"), "yearly price in summary");
    }

    @Test
    void pricingPathWithTrailingSlash_alsoGetsPricingData() {
        IndexHtmlResponse response = responseForPath("/pricing/");

        listener.injectSeo(response);

        assertTrue(response.getDocument().head().html().contains("application/ld+json"));
    }

    @Test
    void nullPathInfo_getsNavButNoPricingData() {
        IndexHtmlResponse response = responseForPath(null);

        listener.injectSeo(response);

        assertTrue(response.getDocument().body().html().contains("<noscript>"), "nav still injected");
        assertFalse(response.getDocument().head().html().contains("application/ld+json"));
    }
}
