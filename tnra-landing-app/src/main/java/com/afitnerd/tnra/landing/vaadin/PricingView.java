package com.afitnerd.tnra.landing.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;

/**
 * Public pricing page. Tiers are placeholder content for now — fill in real
 * names, prices, and features after reviewing the look and feel.
 */
@Route("pricing")
@PageTitle("Pricing — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class PricingView extends VerticalLayout {

    public PricingView() {
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(LandingChrome.nav(false), buildPricing(), LandingChrome.footer());
    }

    private Component buildPricing() {
        Div section = new Div();
        section.addClassName("pricing-section");

        H1 title = new H1("Pricing");
        title.addClassName("pricing-title");

        Paragraph intro = new Paragraph(
            "Placeholder: a short line about how TNRA is priced goes here. " +
            "Describe the billing model (per member, per group, or flat) and any trial."
        );
        intro.addClassName("pricing-intro");

        Div grid = new Div();
        grid.addClassName("pricing-grid");
        grid.add(
            pricingCard("Starter", "$0", "/ month",
                List.of("Placeholder feature one", "Placeholder feature two", "Placeholder feature three"),
                false),
            pricingCard("Group", "$—", "/ month",
                List.of("Everything in Starter", "Placeholder feature", "Placeholder feature", "Placeholder feature"),
                true),
            pricingCard("Community", "Custom", "",
                List.of("Everything in Group", "Placeholder feature", "Placeholder feature"),
                false)
        );

        section.add(title, intro, grid);
        return section;
    }

    private Component pricingCard(String tier, String price, String period, List<String> features, boolean featured) {
        Div card = new Div();
        card.addClassName("pricing-card");
        if (featured) {
            card.addClassName("pricing-card-featured");
        }

        H3 tierName = new H3(tier);
        tierName.addClassName("pricing-tier");

        Div priceRow = new Div();
        priceRow.addClassName("pricing-price-row");
        Span priceEl = new Span(price);
        priceEl.addClassName("pricing-price");
        priceRow.add(priceEl);
        if (!period.isEmpty()) {
            Span periodEl = new Span(" " + period);
            periodEl.addClassName("pricing-period");
            priceRow.add(periodEl);
        }

        Div featureList = new Div();
        featureList.addClassName("pricing-features");
        features.forEach(f -> {
            Paragraph feature = new Paragraph(f);
            feature.addClassName("pricing-feature");
            featureList.add(feature);
        });

        Anchor cta = new Anchor("/#request-access", "Request Access");
        cta.setRouterIgnore(true);
        cta.addClassName("pricing-cta");

        card.add(tierName, priceRow, featureList, cta);
        return card;
    }
}
