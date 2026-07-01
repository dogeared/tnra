package com.afitnerd.tnra.landing.seo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Makes the public landing pages legible to clients that don't run JavaScript.
 *
 * <p>Every landing page is a Vaadin view: the navigation and the pricing both live in the DOM
 * only <em>after</em> the client-side bootstrap runs. A non-JS crawler — e.g. a payment
 * provider's automated domain checker — fetching the raw HTML lands on an empty shell with no
 * links, so it can neither discover the pages nor read the prices. This listener adds two things
 * to the server HTML:
 *
 * <ul>
 *   <li><b>Every page</b> gets a {@code <noscript>} navigation block linking to the public pages,
 *       so a crawler that only follows links can reach {@code /pricing} from any entry point.</li>
 *   <li><b>The home page and the pricing page</b> additionally get schema.org {@code Product}/
 *       {@code Offer} JSON-LD and a human-readable pricing summary, so the prices, the 60-day trial,
 *       and the tax note are present without executing JavaScript. The home page is included because
 *       a payment provider registers the bare domain ({@code tnra.app}) and its automated checker
 *       fetches the apex — which otherwise carries no price at all.</li>
 * </ul>
 *
 * <p>Keep the pricing figures here in sync with {@code content/pricing.md}: an MoR (Paddle)
 * requires the price shown on the site to match the price the buyer sees at checkout.
 */
@Component
public class LandingSeoListener implements VaadinServiceInitListener {

    static final String PRICING_PATH = "/pricing";

    static final String NAV =
        "<nav aria-label=\"TNRA site navigation\">"
        + "<a href=\"/\">Home</a>"
        + "<a href=\"/pricing\">Pricing</a>"
        + "<a href=\"/about-us\">About Us</a>"
        + "<a href=\"/fine-print\">The Fine Print</a>"
        + "<a href=\"/privacy-policy\">Privacy Policy</a>"
        + "<a href=\"/terms-of-service\">Terms of Service</a>"
        + "<a href=\"/refund-policy\">Refund Policy</a>"
        + "</nav>";

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

    static final String PRICING_SUMMARY =
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
        event.addIndexHtmlRequestListener(this::injectSeo);
    }

    void injectSeo(IndexHtmlResponse response) {
        // Every page: crawlable navigation so a non-JS crawler can discover /pricing and the rest.
        Element noscript = response.getDocument().body().appendElement("noscript");
        noscript.append(NAV);

        // Home page and pricing page: structured pricing data + a human-readable summary, so the
        // apex (the URL a payment provider actually checks) and /pricing both carry the price.
        if (wantsPricingData(response)) {
            Element script = response.getDocument().head().appendElement("script");
            script.attr("type", "application/ld+json");
            script.appendChild(new DataNode(JSON_LD));
            noscript.append(PRICING_SUMMARY);
        }
    }

    private boolean wantsPricingData(IndexHtmlResponse response) {
        VaadinRequest request = response.getVaadinRequest();
        String path = request == null ? null : request.getPathInfo();
        return isHomePath(path) || isPricingPath(path);
    }

    private boolean isHomePath(String path) {
        // The Vaadin bootstrap for the "" route reports the apex as null, "", or "/".
        return path == null || path.isEmpty() || "/".equals(path);
    }

    private boolean isPricingPath(String path) {
        return PRICING_PATH.equals(path) || (PRICING_PATH + "/").equals(path);
    }
}
