package com.afitnerd.tnra.landing.content;

import com.afitnerd.tnra.landing.content.LandingContentParser.Block;
import com.afitnerd.tnra.landing.content.LandingContentParser.Card;
import com.afitnerd.tnra.landing.content.LandingContentParser.CardsBlock;
import com.afitnerd.tnra.landing.service.MarkdownService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Renders parsed {@link Block}s into Vaadin components. Static blocks (markdown,
 * hero, cards, cta) become themed {@link Html}; the interactive {@code form} block
 * is delegated to a caller-supplied factory so it can return a live component.
 */
@org.springframework.stereotype.Component
public class LandingContentRenderer {

    private record Variant(String section, String grid, String card, String header, String body, String featured) {
    }

    private static final Map<String, Variant> VARIANTS = Map.of(
        "squares", new Variant("cards-section", "cadence-grid", "cadence-card",
            "cadence-card-header", "cadence-card-body", null),
        "features", new Variant("features-section", "features-grid", "feature-card",
            "feature-heading", "feature-body", null),
        "pricing", new Variant("cards-section", "pricing-grid", "pricing-card",
            "pricing-tier", "pricing-card-body", "pricing-card-featured")
    );

    private final MarkdownService markdown;

    public LandingContentRenderer(MarkdownService markdown) {
        this.markdown = markdown;
    }

    /**
     * @param blocks        parsed content blocks
     * @param customFactory builds components for non-built-in block types (e.g.
     *                      {@code form}); may return null to skip. May be null.
     */
    public List<Component> render(List<Block> blocks, Function<Block, Component> customFactory) {
        List<Component> components = new ArrayList<>();
        for (Block block : blocks) {
            Component component = switch (block.type()) {
                case "markdown" -> section("content-section", "content-prose", markdown.toHtml(block.body()));
                case "hero" -> heroBlock(block);
                case "cta" -> new Html("<div class=\"content-cta\">" + markdown.toHtml(block.body()) + "</div>");
                case "cards" -> cardsBlock(block);
                default -> customFactory == null ? null : customFactory.apply(block);
            };
            if (component != null) {
                components.add(component);
            }
        }
        return components;
    }

    private Component section(String sectionClass, String innerClass, String innerHtml) {
        return new Html("<div class=\"" + sectionClass + "\"><div class=\"" + innerClass + "\">"
            + innerHtml + "</div></div>");
    }

    private Component heroBlock(Block block) {
        return new Html("<div class=\"landing-hero\"><div class=\"hero-inner\">"
            + markdown.toHtml(block.body()) + "</div></div>");
    }

    private Component cardsBlock(Block block) {
        Variant variant = VARIANTS.getOrDefault(block.args(), VARIANTS.get("squares"));
        CardsBlock parsed = LandingContentParser.parseCards(block.body());

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"").append(variant.section()).append("\">");
        if (!parsed.intro().isBlank()) {
            html.append("<div class=\"cards-intro\">").append(markdown.toHtml(parsed.intro())).append("</div>");
        }
        html.append("<div class=\"").append(variant.grid())
            .append("\" data-count=\"").append(parsed.cards().size()).append("\">");
        for (Card card : parsed.cards()) {
            html.append(renderCard(variant, card));
        }
        html.append("</div></div>");
        return new Html(html.toString());
    }

    private String renderCard(Variant variant, Card card) {
        StringBuilder html = new StringBuilder();
        String cardClass = variant.card();
        if (card.featured() && variant.featured() != null) {
            cardClass += " " + variant.featured();
        }
        html.append("<div class=\"").append(cardClass).append("\">");
        if (card.icon() != null) {
            html.append("<span class=\"feature-icon\">").append(escape(card.icon())).append("</span>");
        }
        html.append("<h3 class=\"").append(variant.header()).append("\">").append(escape(card.title())).append("</h3>");
        html.append("<div class=\"").append(variant.body()).append("\">").append(markdown.toHtml(card.body())).append("</div>");
        html.append("</div>");
        return html.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
