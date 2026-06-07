package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.content.LandingContentParser;
import com.afitnerd.tnra.landing.content.LandingContentRenderer;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public pricing page. All content lives in src/main/resources/content/pricing.md.
 */
@Route("pricing")
@PageTitle("Pricing — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class PricingView extends VerticalLayout {

    static final String CONTENT_RESOURCE = "/content/pricing.md";

    public PricingView(MarkdownService markdownService, LandingContentRenderer renderer) {
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(LandingChrome.nav());
        renderer.render(
            LandingContentParser.parse(markdownService.readClasspathResource(CONTENT_RESOURCE)),
            null
        ).forEach(this::add);
        add(LandingChrome.footer());
    }
}
