package com.afitnerd.tnra.landing.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * Shared nav + footer for the public landing pages so the two views
 * ({@link LandingView}, {@link AboutUsView}) stay visually consistent and the
 * nav links live in one place.
 */
final class LandingChrome {

    private LandingChrome() {
    }

    /**
     * @param onHomePage when true, the Request Access link is an in-page anchor
     *                   (smooth scroll); otherwise it navigates to the home page
     *                   and jumps to the form.
     */
    static Component nav(boolean onHomePage) {
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

        Anchor requestLink;
        if (onHomePage) {
            requestLink = new Anchor("#request-access", "Request Access");
        } else {
            requestLink = new Anchor("/#request-access", "Request Access");
            // Force a real browser navigation so the home page loads and scrolls
            // to the form, instead of an SPA route that drops the hash fragment.
            requestLink.setRouterIgnore(true);
        }
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
