package com.afitnerd.tnra.landing.seo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Makes the /pricing page's prices visible to clients that don't run JavaScript.
 *
 * <p>The pricing page is a Vaadin view, so its prices only exist in the DOM <em>after</em> the
 * client-side bootstrap runs. A non-JS crawler — e.g. a payment provider's automated domain
 * checker — fetching the raw HTML sees an empty Vaadin shell and concludes there is "no clear
 * pricing on the domain." This listener injects schema.org {@code Product}/{@code Offer} JSON-LD
 * and a {@code <noscript>} summary so the prices, the 60-day trial, and the tax note are present
 * in the server HTML.
 *
 * <p>Keep these figures in sync with {@code content/pricing.md}: an MoR (Paddle) requires the
 * price shown on the site to match the price the buyer sees at checkout.
 */
@Component
public class PricingSeoListener implements VaadinServiceInitListener {

    static final String PRICING_PATH = "/pricing";

    static final String JSON_LD =
        "{\"@context\":\"https://schema.org\",\"@type\":\"Product\","
        + "\"name\":\"TNRA Hosted Membership\","
        + "\"description\":\"Structured accountability app for recovery, faith, and professional "
        + "groups. Hosted per member.\","
        + "\"brand\":{\"@type\":\"Brand\",\"name\":\"TNRA\"},"
        + "\"offers\":["
        + "{\"@type\":\"Offer\",\"name\":\"Hosted per Month\",\"price\":\"7.00\","
        + "\"priceCurrency\":\"USD\",\"url\":\"https://tnra.app/pricing\","
        + "\"description\":\"$7 per member per month after a 60-day free trial. "
        + "Taxes calculated at checkout.\"},"
        + "{\"@type\":\"Offer\",\"name\":\"Hosted per Year\",\"price\":\"60.00\","
        + "\"priceCurrency\":\"USD\",\"url\":\"https://tnra.app/pricing\","
        + "\"description\":\"$60 per member per year after a 60-day free trial. "
        + "Taxes calculated at checkout.\"}"
        + "]}";

    static final String NOSCRIPT =
        "<section id=\"pricing-fallback\">"
        + "<h1>TNRA Pricing</h1>"
        + "<p><strong>Hosted per Month</strong> — $7 / month / member. 60-day free trial, then "
        + "$7/month per member. Taxes calculated at checkout.</p>"
        + "<p><strong>Hosted per Year</strong> — $60 / year / member. 60-day free trial, then "
        + "$60/year per member. Taxes calculated at checkout.</p>"
        + "<p><strong>Self-Hosted</strong> — $0. Open source and free to use.</p>"
        + "</section>";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(this::injectPricingSeo);
    }

    void injectPricingSeo(IndexHtmlResponse response) {
        if (!isPricingPath(response)) {
            return;
        }
        Element script = response.getDocument().head().appendElement("script");
        script.attr("type", "application/ld+json");
        script.appendChild(new DataNode(JSON_LD));

        response.getDocument().body().appendElement("noscript").append(NOSCRIPT);
    }

    private boolean isPricingPath(IndexHtmlResponse response) {
        VaadinRequest request = response.getVaadinRequest();
        String path = request == null ? null : request.getPathInfo();
        return PRICING_PATH.equals(path) || (PRICING_PATH + "/").equals(path);
    }
}
