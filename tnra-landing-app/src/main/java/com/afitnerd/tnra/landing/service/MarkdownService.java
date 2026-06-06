package com.afitnerd.tnra.landing.service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Renders Markdown to HTML for display via Vaadin's light-DOM {@code Html}
 * component, so the output inherits the app theme (DESIGN.md) — unlike Vaadin's
 * {@code Markdown} component, which renders into shadow DOM the theme can't reach.
 *
 * Input is trusted, repo-owned content (files under src/main/resources), never
 * user input, so the rendered HTML is not sanitized.
 */
@Service
public class MarkdownService {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    /** Render a Markdown string to an HTML fragment. */
    public String toHtml(String markdown) {
        if (markdown == null) {
            return "";
        }
        return renderer.render(parser.parse(markdown));
    }

    /**
     * Load a Markdown file from the classpath and render it to HTML.
     *
     * @param classpathResource e.g. {@code "/about-us.md"}
     * @throws IllegalArgumentException if the resource does not exist
     */
    public String renderClasspathResource(String classpathResource) {
        try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Markdown resource not found: " + classpathResource);
            }
            String markdown = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return toHtml(markdown);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read markdown resource: " + classpathResource, e);
        }
    }
}
