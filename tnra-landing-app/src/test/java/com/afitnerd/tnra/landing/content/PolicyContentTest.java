package com.afitnerd.tnra.landing.content;

import com.afitnerd.tnra.landing.service.MarkdownService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the policy content files the legal pages depend on: each must exist on the classpath (a wrong
 * path would 500 the page at runtime, which compilation can't catch), carry its expected H1, and render
 * to non-trivial HTML.
 */
class PolicyContentTest {

    private final MarkdownService markdown = new MarkdownService();

    @Test
    void privacyPolicy_loadsRendersAndHasHeading() {
        assertPolicy("/content/privacy-policy.md", "Privacy Policy");
    }

    @Test
    void termsOfService_loadsRendersAndHasHeading() {
        assertPolicy("/content/terms-of-service.md", "Terms of Service");
    }

    @Test
    void refundPolicy_loadsRendersAndHasHeading() {
        assertPolicy("/content/refund-policy.md", "Refund Policy");
    }

    private void assertPolicy(String resource, String heading) {
        // Throws IllegalArgumentException if the resource is missing — the failure mode we're guarding.
        String raw = markdown.readClasspathResource(resource);
        assertTrue(raw.contains("# " + heading), "expected '# " + heading + "' in " + resource);

        String html = markdown.toHtml(raw);
        assertTrue(html.contains("<h1>" + heading + "</h1>"), "expected rendered <h1> in " + resource);
        assertTrue(html.length() > 200, "expected non-trivial content in " + resource);
    }
}
