package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlackStatsRendererTest {

    private SlackStatsRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new SlackStatsRenderer();
    }

    @Test
    void rendersStatsInDisplayOrderWithEmojiAndLabel() {
        Post post = new Post();
        post.setStatValues(new ArrayList<>(List.of(
            statValue(post, stat("meditate", "Meditate", "🧘", 1), 3),
            statValue(post, stat("exercise", "Exercise", "💪", 0), 5)
        )));

        String out = renderer.render(post);

        assertTrue(out.startsWith("*Stats*\n"));
        int exerciseIdx = out.indexOf("Exercise");
        int meditateIdx = out.indexOf("Meditate");
        assertTrue(exerciseIdx > 0 && meditateIdx > 0);
        assertTrue(exerciseIdx < meditateIdx, "displayOrder=0 must appear before displayOrder=1");
        assertTrue(out.contains("💪 *Exercise:* 5"));
        assertTrue(out.contains("🧘 *Meditate:* 3"));
    }

    @Test
    void skipsArchivedStats() {
        Post post = new Post();
        StatDefinition archived = stat("retired", "Retired", "🪦", 0);
        archived.setArchived(true);
        post.setStatValues(new ArrayList<>(List.of(
            statValue(post, archived, 99),
            statValue(post, stat("read", "Read", "📚", 1), 7)
        )));

        String out = renderer.render(post);

        assertFalse(out.contains("Retired"), "Archived stats must be omitted");
        assertTrue(out.contains("Read"));
    }

    @Test
    void skipsValuesWithNull() {
        Post post = new Post();
        post.setStatValues(new ArrayList<>(List.of(
            statValue(post, stat("exercise", "Exercise", "💪", 0), null),
            statValue(post, stat("pray", "Pray", "🙏", 1), 4)
        )));

        String out = renderer.render(post);

        assertFalse(out.contains("Exercise"));
        assertTrue(out.contains("*Pray:* 4"));
    }

    @Test
    void emptyStatValuesReturnsEmptyString() {
        Post post = new Post();
        post.setStatValues(new ArrayList<>());
        assertEquals("", renderer.render(post));
    }

    @Test
    void nullStatValuesReturnsEmptyString() {
        Post post = new Post();
        post.setStatValues(null);
        assertEquals("", renderer.render(post));
    }

    @Test
    void rendersWithoutEmojiWhenStatHasNone() {
        Post post = new Post();
        StatDefinition noEmoji = stat("custom", "Custom Stat", null, 0);
        post.setStatValues(new ArrayList<>(List.of(statValue(post, noEmoji, 1))));

        String out = renderer.render(post);
        assertTrue(out.contains("*Custom Stat:* 1"));
    }

    @Test
    void fallsBackToNameWhenLabelMissing() {
        Post post = new Post();
        StatDefinition noLabel = stat("only-name", null, "🔥", 0);
        post.setStatValues(new ArrayList<>(List.of(statValue(post, noLabel, 2))));

        String out = renderer.render(post);
        assertTrue(out.contains("*only-name:* 2"));
    }

    private StatDefinition stat(String name, String label, String emoji, int order) {
        StatDefinition def = new StatDefinition(name, label, emoji, order);
        return def;
    }

    private PostStatValue statValue(Post post, StatDefinition def, Integer value) {
        return new PostStatValue(post, def, value);
    }
}
