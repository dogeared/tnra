package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.content.LandingContentParser;
import com.afitnerd.tnra.landing.content.LandingContentRenderer;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * "The Fine Print" — a single page that embeds the Privacy Policy, Terms of Service, and Refund Policy
 * as stacked sections, so everything legal is in one place. Each policy is also available as its own
 * standalone page (linked in the footer); this view renders the same content files.
 *
 * <p>Each section's element id matches its standalone route, so {@code /fine-print?to=refund-policy}
 * scrolls straight to that section (see {@link LandingChrome#scrollOnNavigation}).
 */
@Route("fine-print")
@PageTitle("The Fine Print — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class FinePrintView extends VerticalLayout implements AfterNavigationObserver {

    private final MarkdownService markdownService;
    private final LandingContentRenderer renderer;

    public FinePrintView(MarkdownService markdownService, LandingContentRenderer renderer) {
        this.markdownService = markdownService;
        this.renderer = renderer;

        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(LandingChrome.nav());
        add(new Html("<div class=\"content-section\"><div class=\"content-prose\">"
            + "<h1>The Fine Print</h1>"
            + "<p>Our Privacy Policy, Terms of Service, and Refund Policy — all in one place. "
            + "Each is also available as its own page (linked in the footer).</p>"
            + "</div></div>"));
        add(section("privacy-policy", PrivacyPolicyView.CONTENT_RESOURCE));
        add(section("terms-of-service", TermsOfServiceView.CONTENT_RESOURCE));
        add(section("refund-policy", RefundPolicyView.CONTENT_RESOURCE));
        add(LandingChrome.footer());
    }

    /** Render one policy file into a divided, anchorable section (id = its standalone route). */
    private Div section(String id, String contentResource) {
        Div section = new Div();
        section.setId(id);
        section.addClassName("fine-print-section");
        renderer.render(
            LandingContentParser.parse(markdownService.readClasspathResource(contentResource)),
            null
        ).forEach(section::add);
        return section;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        LandingChrome.scrollOnNavigation(this, event);
    }
}
