package com.afitnerd.tnra.landing.content;

import com.afitnerd.tnra.landing.content.LandingContentParser.Block;
import com.afitnerd.tnra.landing.content.LandingContentParser.CardsBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandingContentParserTest {

    @Test
    void plainTextBecomesASingleMarkdownBlock() {
        List<Block> blocks = LandingContentParser.parse("# Hello\n\nSome prose.");
        assertEquals(1, blocks.size());
        assertEquals("markdown", blocks.get(0).type());
        assertTrue(blocks.get(0).body().contains("# Hello"));
    }

    @Test
    void emptyContentYieldsNoBlocks() {
        assertTrue(LandingContentParser.parse("").isEmpty());
        assertTrue(LandingContentParser.parse(null).isEmpty());
        assertTrue(LandingContentParser.parse("\n\n   \n").isEmpty());
    }

    @Test
    void fencedBlockIsExtractedWithTypeAndArgs() {
        List<Block> blocks = LandingContentParser.parse(
            "intro prose\n\n:::cards squares\n:: Daily\nbody\n:::\n\noutro prose");

        assertEquals(3, blocks.size());
        assertEquals("markdown", blocks.get(0).type());
        assertEquals("cards", blocks.get(1).type());
        assertEquals("squares", blocks.get(1).args());
        assertTrue(blocks.get(1).body().contains(":: Daily"));
        assertEquals("markdown", blocks.get(2).type());
    }

    @Test
    void fenceWithoutArgsHasNullArgs() {
        List<Block> blocks = LandingContentParser.parse(":::form\ntitle: Hi\n:::");
        assertEquals(1, blocks.size());
        assertEquals("form", blocks.get(0).type());
        assertNull(blocks.get(0).args());
    }

    @Test
    void parseCardsSplitsIntroAndCards() {
        CardsBlock cb = LandingContentParser.parseCards(
            "## Section title\n\nintro line\n:: Daily\nDaily body\n:: Weekly\nWeekly body");

        assertTrue(cb.intro().contains("## Section title"));
        assertEquals(2, cb.cards().size());
        assertEquals("Daily", cb.cards().get(0).title());
        assertEquals("Daily body", cb.cards().get(0).body());
        assertEquals("Weekly", cb.cards().get(1).title());
    }

    @Test
    void parseCardsExtractsLeadingEmojiAsIcon() {
        CardsBlock cb = LandingContentParser.parseCards(":: 📝 Structured weekly posts\nbody");
        assertEquals("📝", cb.cards().get(0).icon());
        assertEquals("Structured weekly posts", cb.cards().get(0).title());
    }

    @Test
    void parseCardsDetectsFeaturedFlag() {
        CardsBlock cb = LandingContentParser.parseCards(":: Group {featured}\nbody\n:: Starter\nbody");
        assertTrue(cb.cards().get(0).featured());
        assertEquals("Group", cb.cards().get(0).title());
        assertFalse(cb.cards().get(1).featured());
        assertNull(cb.cards().get(1).icon());
    }

    @Test
    void parseFieldsReadsKeyValueLines() {
        Map<String, String> fields = LandingContentParser.parseFields(
            "title: Request access\nsubmit: Send Request\nsuccess: Thanks at {email}.");
        assertEquals("Request access", fields.get("title"));
        assertEquals("Send Request", fields.get("submit"));
        assertEquals("Thanks at {email}.", fields.get("success"));
    }
}
