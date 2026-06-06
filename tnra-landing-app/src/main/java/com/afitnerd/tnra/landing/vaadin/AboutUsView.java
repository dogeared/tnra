package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Public "About Us" page. Content is authored in src/main/resources/about-us.md
 * and rendered to themed HTML at request time.
 */
@Route("about-us")
@PageTitle("About Us — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class AboutUsView extends VerticalLayout {

    static final String ABOUT_US_RESOURCE = "/about-us.md";

    public AboutUsView(MarkdownService markdownService) {
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        Div section = new Div();
        section.addClassName("about-section");

        Div article = new Div();
        article.addClassName("about-markdown");
        String html = markdownService.renderClasspathResource(ABOUT_US_RESOURCE);
        article.add(new Html("<div>" + html + "</div>"));

        section.add(article);

        add(LandingChrome.nav(false), section, LandingChrome.footer());
    }
}
