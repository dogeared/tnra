package com.afitnerd.tnra.landing.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;

import java.util.List;

/**
 * Shared nav + footer for the public landing pages so the views
 * ({@link LandingView}, {@link AboutUsView}, {@link PricingView}) stay visually
 * consistent and the nav links live in one place.
 */
final class LandingChrome {

    /** Query param that scrolls a landing page to a section: {@code /?to=<id>}. */
    static final String SCROLL_PARAM = "to";
    /** Element id of the Request Access form section. */
    static final String SCROLL_FORM = "request-access";

    private LandingChrome() {
    }

    /**
     * Shared post-navigation scroll for all landing pages: scroll to the child
     * section whose element id matches {@code ?to=<id>}, otherwise to the top of the
     * page (so SPA navigation never strands you mid-page at the previous scroll
     * position). Vaadin defers the scroll until the view is rendered client-side.
     */
    static void scrollOnNavigation(VerticalLayout view, AfterNavigationEvent event) {
        List<String> to = event.getLocation().getQueryParameters().getParameters()
            .getOrDefault(SCROLL_PARAM, List.of());
        Component target = to.isEmpty() ? null : view.getChildren()
            .filter(c -> c.getId().map(to::contains).orElse(false))
            .findFirst().orElse(null);
        if (target != null) {
            target.scrollIntoView();
        } else {
            view.scrollIntoView();
        }
    }

    static Component nav() {
        HorizontalLayout nav = new HorizontalLayout();
        nav.addClassName("landing-nav");
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.setJustifyContentMode(JustifyContentMode.BETWEEN);

        RouterLink logo = new RouterLink("TNRA", LandingView.class);
        logo.addClassName("nav-logo");

        RouterLink aboutLink = new RouterLink("About Us", AboutUsView.class);
        aboutLink.addClassName("nav-link");

        RouterLink pricingLink = new RouterLink("Pricing", PricingView.class);
        pricingLink.addClassName("nav-link");

        // Routes to the home page with ?to=request-access; LandingView scrolls the
        // form into view server-side (works from any page, no fragment timing issue).
        RouterLink requestLink = new RouterLink("Request Access", LandingView.class);
        requestLink.setQueryParameters(QueryParameters.of(SCROLL_PARAM, SCROLL_FORM));
        requestLink.addClassName("nav-link");

        HorizontalLayout links = new HorizontalLayout(aboutLink, pricingLink, requestLink);
        links.addClassName("nav-links");
        links.setAlignItems(Alignment.CENTER);
        links.setSpacing(false);

        nav.add(logo, links);
        return nav;
    }

    static Component footer() {
        Footer footer = new Footer();
        footer.addClassName("landing-footer");
        footer.add(new Paragraph("© 2026 TNRA. Built for groups that take commitment seriously."));
        return footer;
    }
}
