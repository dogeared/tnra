package com.afitnerd.tnra.landing.seo;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
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
 *   <li><b>Every page</b> gets Open Graph + Twitter Card meta tags, so a link pasted into Slack,
 *       LinkedIn, iMessage, etc. unfurls with the TNRA banner, title and description. Those scrapers
 *       don't run JavaScript either, and the image URL must be absolute and anonymously fetchable
 *       (see {@code SecurityConfig}, which permits {@code /images/**}).</li>
 * </ul>
 *
 * <p>Keep the pricing figures here in sync with {@code content/pricing.md}: an MoR (Paddle)
 * requires the price shown on the site to match the price the buyer sees at checkout.
 */
@Component
public class LandingSeoListener implements VaadinServiceInitListener {

    static final String PRICING_PATH = "/pricing";

    static final String OG_TITLE = "TNRA — Accountability for Groups That Mean It";
    static final String OG_DESCRIPTION =
        "Taking the Next Right Action: a structured home for weekly posts, group stats, and mutual "
        + "visibility for recovery groups, faith communities, and professional accountability circles — "
        + "end-to-end encrypted.";
    /** 1200×630, the size Slack, LinkedIn, Facebook and X all render as a large card. */
    static final String OG_IMAGE_PATH = "/images/og-banner.png";
    static final String OG_IMAGE_ALT = "TNRA — Accountability for groups that mean it.";

    private final String baseUrl;

    /**
     * @param baseUrl public origin of the landing site, used for absolute {@code og:url}/{@code og:image}
     *                (scrapers reject relative image URLs); defaults to production
     */
    public LandingSeoListener(@Value("${tnra.site.base-url:https://tnra.app}") String baseUrl) {
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

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
        // Every page: link-preview metadata for Slack/LinkedIn/etc.
        injectOpenGraph(response);

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

    /**
     * Open Graph uses {@code property=}, Twitter Cards use {@code name=}; both sets are emitted because
     * Slack/LinkedIn/Facebook read OG while X reads its own, and sites commonly ship both.
     */
    private void injectOpenGraph(IndexHtmlResponse response) {
        Element head = response.getDocument().head();
        String url = baseUrl + canonicalPath(response);
        String image = baseUrl + OG_IMAGE_PATH;

        property(head, "og:type", "website");
        property(head, "og:site_name", "TNRA");
        property(head, "og:title", OG_TITLE);
        property(head, "og:description", OG_DESCRIPTION);
        property(head, "og:url", url);
        property(head, "og:image", image);
        property(head, "og:image:width", "1200");
        property(head, "og:image:height", "630");
        property(head, "og:image:alt", OG_IMAGE_ALT);

        name(head, "twitter:card", "summary_large_image");
        name(head, "twitter:title", OG_TITLE);
        name(head, "twitter:description", OG_DESCRIPTION);
        name(head, "twitter:image", image);
        name(head, "twitter:image:alt", OG_IMAGE_ALT);
    }

    private static void property(Element head, String property, String content) {
        head.appendElement("meta").attr("property", property).attr("content", content);
    }

    private static void name(Element head, String name, String content) {
        head.appendElement("meta").attr("name", name).attr("content", content);
    }

    /** The request path with the apex normalised to {@code /} and a trailing slash dropped elsewhere. */
    String canonicalPath(IndexHtmlResponse response) {
        VaadinRequest request = response.getVaadinRequest();
        String path = request == null ? null : request.getPathInfo();
        if (isHomePath(path)) {
            return "/";
        }
        String p = path.startsWith("/") ? path : "/" + path;
        return p.length() > 1 && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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
