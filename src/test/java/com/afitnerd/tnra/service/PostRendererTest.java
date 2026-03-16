package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostRendererTest {

    @Test
    void utf8ToAsciiConvertsSmartQuotes() {
        String input = "It’s a test with ‛single‛ and ”double‟ quotes";
        String expected = "It's a test with 'single' and \"double\" quotes";

        assertEquals(expected, PostRenderer.utf8ToAscii(input));
    }

    @Test
    void utf8ToAsciiReturnsNullForNullInput() {
        assertNull(PostRenderer.utf8ToAscii(null));
    }
}
