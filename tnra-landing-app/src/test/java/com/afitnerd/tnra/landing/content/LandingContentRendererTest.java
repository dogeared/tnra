package com.afitnerd.tnra.landing.content;

import com.afitnerd.tnra.landing.content.LandingContentParser.Block;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingContentRendererTest {

    private final LandingContentRenderer renderer = new LandingContentRenderer(new MarkdownService());

    private String renderOne(String content) {
        List<Component> components = renderer.render(LandingContentParser.parse(content), null);
        assertEquals(1, components.size());
        return components.get(0).getElement().getOuterHTML();
    }

    @Test
    void markdownBlockRendersThemedProse() {
        String html = renderOne("# Title\n\nSome prose.");
        assertTrue(html.contains("content-section"), html);
        assertTrue(html.contains("content-prose"), html);
        assertTrue(html.contains("<h1>Title</h1>"), html);
    }

    @Test
    void heroBlockRendersHeadlineAndInner() {
        String html = renderOne(":::hero\n# Big\n\nsubtext\n:::");
        assertTrue(html.contains("landing-hero"), html);
        assertTrue(html.contains("hero-inner"), html);
        assertTrue(html.contains("<h1>Big</h1>"), html);
    }

    @Test
    void ctaBlockRendersLink() {
        String html = renderOne(":::cta\n[Request Access](#request-access)\n:::");
        assertTrue(html.contains("content-cta"), html);
        assertTrue(html.contains("href=\"#request-access\""), html);
    }

    @Test
    void squaresCardsRenderWithIntroAndCards() {
        String html = renderOne(":::cards squares\n## The TNRA Way\nintro\n### Cadence\n:: Daily\nbody\n:: Weekly\nbody\n:::");
        assertTrue(html.contains("cards-section"), html);
        assertTrue(html.contains("cards-intro"), html);
        assertTrue(html.contains("cadence-grid"), html);
        assertTrue(html.contains("cadence-card-header"), html);
        assertTrue(html.contains("Daily"), html);
        assertTrue(html.contains("Weekly"), html);
    }

    @Test
    void featureCardsRenderIconFromEmoji() {
        String html = renderOne(":::cards features\n:: 📝 Structured posts\nbody\n:::");
        assertTrue(html.contains("features-section"), html);
        assertTrue(html.contains("feature-card"), html);
        assertTrue(html.contains("feature-icon"), html);
        assertTrue(html.contains("📝"), html);
        assertTrue(html.contains("Structured posts"), html);
    }

    @Test
    void pricingCardsMarkTheFeaturedTier() {
        String html = renderOne(":::cards pricing\n:: Group {featured}\n**$1**\n:: Starter\n**$0**\n:::");
        assertTrue(html.contains("pricing-grid"), html);
        assertTrue(html.contains("pricing-card-featured"), html);
        assertTrue(html.contains("pricing-tier"), html);
        assertTrue(html.contains("pricing-card-body"), html);
    }

    @Test
    void featuredFlagIsIgnoredForNonPricingVariants() {
        // squares has no "featured" style, so {featured} renders a normal card.
        String html = renderOne(":::cards squares\n:: Daily {featured}\nbody\n:::");
        assertTrue(html.contains("cadence-card"), html);
        assertTrue(!html.contains("featured"), html);
    }

    @Test
    void cardGridExposesCardCount() {
        // data-count drives the even-wrap CSS (e.g. 4 squares -> 2x2).
        String html = renderOne(":::cards squares\n:: A\nx\n:: B\ny\n:: C\nz\n:::");
        assertTrue(html.contains("data-count=\"3\""), html);
    }

    @Test
    void anchorIdParsesHashTokenAndVariantIgnoresIt() {
        assertEquals("the-tnra-way", LandingContentRenderer.anchorId("squares #the-tnra-way"));
        assertEquals("squares", LandingContentRenderer.firstArg("squares #the-tnra-way"));
        assertEquals("the-tnra-way", LandingContentRenderer.anchorId("#the-tnra-way"));
        assertEquals(null, LandingContentRenderer.anchorId("squares"));
        assertEquals(null, LandingContentRenderer.anchorId(null));
    }

    @Test
    void blockAnchorBecomesComponentId() {
        List<Component> out = renderer.render(
            LandingContentParser.parse(":::cards squares #the-tnra-way\n:: Daily\nbody\n:::"), null);
        assertEquals(1, out.size());
        assertEquals("the-tnra-way", out.get(0).getId().orElse(""));
        // variant still resolves (squares -> cadence-grid) even with the anchor present
        assertTrue(out.get(0).getElement().getOuterHTML().contains("cadence-grid"));
    }

    @Test
    void unknownVariantFallsBackToSquares() {
        String html = renderOne(":::cards bogus\n:: A\nbody\n:::");
        assertTrue(html.contains("cadence-grid"), html);
    }

    @Test
    void cardTitleIsHtmlEscaped() {
        String html = renderOne(":::cards squares\n:: A <b> & C\nbody\n:::");
        assertTrue(html.contains("A &lt;b&gt; &amp; C"), html);
    }

    @Test
    void formBlockIsDelegatedToTheFactory() {
        Function<Block, Component> factory = block -> {
            Div marker = new Div();
            marker.setId("form-" + block.type());
            return marker;
        };
        List<Component> out = renderer.render(LandingContentParser.parse(":::form\ntitle: Hi\n:::"), factory);
        assertEquals(1, out.size());
        assertEquals("form-form", out.get(0).getId().orElse(""));
    }

    @Test
    void formBlockSkippedWhenNoFactory() {
        assertTrue(renderer.render(LandingContentParser.parse(":::form\ntitle: Hi\n:::"), null).isEmpty());
    }

    @Test
    void factoryReturningNullIsSkipped() {
        assertTrue(renderer.render(LandingContentParser.parse(":::form\nx\n:::"), block -> null).isEmpty());
    }

    @Test
    void rendersAllBlocksInOrder() {
        List<Component> out = renderer.render(
            LandingContentParser.parse(":::hero\n# H\n:::\n\nprose\n\n:::cta\n[a](#b)\n:::"), null);
        assertEquals(3, out.size());
        assertTrue(out.get(0).getElement().getOuterHTML().contains("landing-hero"));
        assertTrue(out.get(1).getElement().getOuterHTML().contains("content-section"));
        assertTrue(out.get(2).getElement().getOuterHTML().contains("content-cta"));
    }
}
