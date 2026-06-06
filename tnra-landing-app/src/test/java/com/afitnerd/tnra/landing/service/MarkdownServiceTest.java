package com.afitnerd.tnra.landing.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownServiceTest {

    private final MarkdownService service = new MarkdownService();

    @Test
    void toHtmlRendersCommonElements() {
        String html = service.toHtml("# Title\n\nSome **bold** text.\n\n- one\n- two");
        assertTrue(html.contains("<h1>Title</h1>"), html);
        assertTrue(html.contains("<strong>bold</strong>"), html);
        assertTrue(html.contains("<ul>") && html.contains("<li>one</li>"), html);
    }

    @Test
    void toHtmlRendersLinks() {
        String html = service.toHtml("[Request access](/#request-access)");
        assertTrue(html.contains("<a href=\"/#request-access\">Request access</a>"), html);
    }

    @Test
    void toHtmlReturnsEmptyForNull() {
        assertEquals("", service.toHtml(null));
    }

    @Test
    void renderClasspathResourceRendersTheAboutPage() {
        String html = service.renderClasspathResource("/content/about-us.md");
        assertTrue(html.contains("<h1>About TNRA</h1>"), html);
        assertTrue(html.contains("<blockquote>"), html);
    }

    @Test
    void readClasspathResourceReturnsRawMarkdown() {
        String raw = service.readClasspathResource("/content/about-us.md");
        assertTrue(raw.contains("# About TNRA"), raw);
    }

    @Test
    void renderClasspathResourceThrowsWhenMissing() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> service.renderClasspathResource("/does-not-exist.md")
        );
        assertTrue(ex.getMessage().contains("does-not-exist.md"));
    }
}
