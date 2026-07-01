package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.content.LandingContentParser;
import com.afitnerd.tnra.landing.content.LandingContentRenderer;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Standalone Terms of Service page. Content lives in src/main/resources/content/terms-of-service.md and
 * is also embedded (as a section) on {@link FinePrintView}.
 */
@Route("terms-of-service")
@PageTitle("Terms of Service — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class TermsOfServiceView extends VerticalLayout implements AfterNavigationObserver {

    static final String CONTENT_RESOURCE = "/content/terms-of-service.md";

    public TermsOfServiceView(MarkdownService markdownService, LandingContentRenderer renderer) {
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

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        LandingChrome.scrollOnNavigation(this, event);
    }
}
