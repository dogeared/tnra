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
 * Public "About Us" page. Content lives in src/main/resources/content/about-us.md
 * (plain Markdown — the same renderer the landing/pricing pages use).
 */
@Route("about-us")
@PageTitle("About Us — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class AboutUsView extends VerticalLayout {

    static final String CONTENT_RESOURCE = "/content/about-us.md";

    public AboutUsView(MarkdownService markdownService, LandingContentRenderer renderer) {
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
