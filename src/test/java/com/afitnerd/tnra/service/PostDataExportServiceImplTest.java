package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostDataExportServiceImplTest {

    private PostRepository postRepository;
    private PostDataExportServiceImpl service;
    private User user;
    private final AtomicLong nextId = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        service = new PostDataExportServiceImpl(postRepository);
        user = new User("Test", "User", "test@example.com");
        user.setId(1L);
    }

    @Test
    void emptyResult_returnsHeaderOnly() {
        when(postRepository.findByUser(user)).thenReturn(new ArrayList<>());

        String csv = exportAsString(null, null);

        assertTrue(csv.contains("post_id,state,started_at,finished_at"));
        long lines = csv.lines().count();
        assertEquals(1, lines, "Header row only when no posts");
    }

    @Test
    void utf8BomIsAtStart() {
        when(postRepository.findByUser(user)).thenReturn(new ArrayList<>());

        byte[] csv = service.exportToCsv(user, null, null);

        assertEquals((byte) 0xEF, csv[0]);
        assertEquals((byte) 0xBB, csv[1]);
        assertEquals((byte) 0xBF, csv[2]);
    }

    @Test
    void rowIncludesAllBaseFields() {
        Post post = simplePost("2026-05-10");
        Intro intro = new Intro();
        intro.setWidwytk("hidden");
        intro.setKryptonite("weak");
        intro.setWhatAndWhen("plan");
        post.setIntro(intro);
        post.setPersonal(cat("p-best", "p-worst"));
        post.setFamily(cat("f-best", "f-worst"));
        post.setWork(cat("w-best", "w-worst"));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        assertTrue(csv.contains("hidden"));
        assertTrue(csv.contains("weak"));
        assertTrue(csv.contains("plan"));
        assertTrue(csv.contains("p-best"));
        assertTrue(csv.contains("p-worst"));
        assertTrue(csv.contains("f-best"));
        assertTrue(csv.contains("w-best"));
    }

    @Test
    void statColumnHeaderUsesEmojiPlusLabel() {
        Post post = simplePost("2026-05-10");
        StatDefinition exercise = statDef("exercise", "Exercise", "💪", 0);
        post.setStatValues(new ArrayList<>(List.of(new PostStatValue(post, exercise, 5))));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String header = exportAsString(null, null).lines().findFirst().orElseThrow();

        assertTrue(header.contains("💪 Exercise"), "Header should include emoji prefix + label: " + header);
    }

    @Test
    void statColumnHeaderFallsBackToLabelWhenNoEmoji() {
        Post post = simplePost("2026-05-10");
        StatDefinition stat = statDef("read", "Read", null, 0);
        post.setStatValues(new ArrayList<>(List.of(new PostStatValue(post, stat, 4))));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String header = exportAsString(null, null).lines().findFirst().orElseThrow();

        assertTrue(header.endsWith(",Read"), "Tail of header should be plain label: " + header);
    }

    @Test
    void statColumnHeaderFallsBackToNameWhenLabelMissing() {
        Post post = simplePost("2026-05-10");
        StatDefinition stat = statDef("custom-stat", null, null, 0);
        post.setStatValues(new ArrayList<>(List.of(new PostStatValue(post, stat, 1))));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String header = exportAsString(null, null).lines().findFirst().orElseThrow();

        assertTrue(header.endsWith(",custom-stat"), "Tail of header should fall back to name: " + header);
    }

    @Test
    void statColumnsOrderedByDisplayOrder() {
        Post post = simplePost("2026-05-10");
        StatDefinition meditate = statDef("meditate", "Meditate", "🧘", 1);
        StatDefinition exercise = statDef("exercise", "Exercise", "💪", 0);
        post.setStatValues(new ArrayList<>(List.of(
            new PostStatValue(post, meditate, 3),
            new PostStatValue(post, exercise, 5)
        )));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String header = exportAsString(null, null).lines().findFirst().orElseThrow();

        int exerciseIdx = header.indexOf("Exercise");
        int meditateIdx = header.indexOf("Meditate");
        assertTrue(exerciseIdx > 0 && meditateIdx > 0);
        assertTrue(exerciseIdx < meditateIdx, "display_order=0 must appear before display_order=1: " + header);
    }

    @Test
    void archivedStatsIncludedIfUserHasValues() {
        Post post = simplePost("2026-05-10");
        StatDefinition retired = statDef("retired", "Retired", "🪦", 0);
        retired.setArchived(true);
        post.setStatValues(new ArrayList<>(List.of(new PostStatValue(post, retired, 99))));
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        assertTrue(csv.contains("Retired"));
        assertTrue(csv.contains("99"));
    }

    @Test
    void postWithoutStatValueLeavesEmptyCell() {
        StatDefinition exercise = statDef("exercise", "Exercise", "💪", 0);
        Post withStat = simplePost("2026-05-10");
        withStat.setStatValues(new ArrayList<>(List.of(new PostStatValue(withStat, exercise, 7))));
        Post withoutStat = simplePost("2026-05-09");
        when(postRepository.findByUser(user)).thenReturn(List.of(withStat, withoutStat));

        String csv = exportAsString(null, null);
        // 1 header + 2 rows
        long lines = csv.lines().count();
        assertEquals(3, lines);
        // The row without the stat should end with a trailing empty field
        List<String> rows = csv.lines().toList();
        assertTrue(rows.get(2).endsWith(",") || rows.get(2).endsWith(",\""),
            "Row without stat should have empty trailing cell: " + rows.get(2));
    }

    @Test
    void newestPostFirst() {
        Post older = simplePost("2026-05-01");
        Post newer = simplePost("2026-05-15");
        when(postRepository.findByUser(user)).thenReturn(List.of(older, newer));

        List<String> rows = exportAsString(null, null).lines().toList();
        // rows[0]=header, rows[1]=newer post, rows[2]=older post
        assertEquals(3, rows.size());
        assertTrue(rows.get(1).startsWith(String.valueOf(newer.getId())),
            "Newest post should be first data row: " + rows.get(1));
    }

    @Test
    void dateRangeFromFilter() {
        Post inRange = simplePost("2026-05-10");
        Post tooOld = simplePost("2026-05-01");
        when(postRepository.findByUser(user)).thenReturn(List.of(inRange, tooOld));

        String csv = exportAsString(LocalDate.of(2026, 5, 5), null);

        long dataRows = csv.lines().count() - 1;
        assertEquals(1, dataRows);
        assertTrue(csv.contains(String.valueOf(inRange.getId())));
        assertFalse(csv.contains("\n" + tooOld.getId() + ","));
    }

    @Test
    void dateRangeToFilterIsInclusive() {
        Post boundary = simplePost("2026-05-10");
        Post past = simplePost("2026-05-12");
        when(postRepository.findByUser(user)).thenReturn(List.of(boundary, past));

        String csv = exportAsString(null, LocalDate.of(2026, 5, 10));

        long dataRows = csv.lines().count() - 1;
        assertEquals(1, dataRows);
        assertTrue(csv.contains(String.valueOf(boundary.getId())),
            "End-of-day inclusive bound should include posts on the same day");
    }

    @Test
    void dateRangeBothBounds() {
        Post inRange = simplePost("2026-05-10");
        Post before = simplePost("2026-04-30");
        Post after = simplePost("2026-05-20");
        when(postRepository.findByUser(user)).thenReturn(List.of(inRange, before, after));

        String csv = exportAsString(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15));

        assertEquals(2L, csv.lines().count(), "Header + 1 row");
        assertTrue(csv.contains(String.valueOf(inRange.getId())));
    }

    @Test
    void postWithNullStartDateOmittedWhenRangeBounded() {
        Post nullStart = simplePost(null);
        Post valid = simplePost("2026-05-10");
        when(postRepository.findByUser(user)).thenReturn(List.of(nullStart, valid));

        String csv = exportAsString(LocalDate.of(2026, 1, 1), null);

        assertEquals(2L, csv.lines().count(), "Null-start post is skipped under bounded filter");
    }

    @Test
    void multiLineValueIsProperlyQuoted() {
        Post post = simplePost("2026-05-10");
        Intro intro = new Intro();
        intro.setWidwytk("line one\nline two\nline three");
        post.setIntro(intro);
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        // Multi-line value must be wrapped in quotes so the CSV row stays cohesive
        assertTrue(csv.contains("\"line one\nline two\nline three\""),
            "Multi-line text must be quoted to keep the row intact");
    }

    @Test
    void valueWithCommaIsQuoted() {
        Post post = simplePost("2026-05-10");
        Intro intro = new Intro();
        intro.setKryptonite("one, two, three");
        post.setIntro(intro);
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        assertTrue(csv.contains("\"one, two, three\""));
    }

    @Test
    void valueWithQuoteIsEscaped() {
        Post post = simplePost("2026-05-10");
        Intro intro = new Intro();
        intro.setWhatAndWhen("she said \"hi\"");
        post.setIntro(intro);
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        // RFC-4180: embedded quotes are doubled
        assertTrue(csv.contains("\"she said \"\"hi\"\"\""));
    }

    @Test
    void startedAtFormattedInUtc() {
        Post post = simplePost("2026-05-10");
        when(postRepository.findByUser(user)).thenReturn(List.of(post));

        String csv = exportAsString(null, null);

        assertTrue(csv.contains("2026-05-10 00:00:00 UTC"),
            "Started-at column should render in UTC: " + csv);
    }

    @Test
    void columnHeaderForHelperCovered() {
        StatDefinition full = statDef("name", "Label", "🔥", 0);
        assertEquals("🔥 Label", PostDataExportServiceImpl.columnHeaderFor(full));

        StatDefinition noEmoji = statDef("name", "Label", null, 0);
        assertEquals("Label", PostDataExportServiceImpl.columnHeaderFor(noEmoji));

        StatDefinition noLabel = statDef("only-name", null, null, 0);
        assertEquals("only-name", PostDataExportServiceImpl.columnHeaderFor(noLabel));

        StatDefinition blankLabel = statDef("fallback", "  ", "🌟", 0);
        assertEquals("🌟 fallback", PostDataExportServiceImpl.columnHeaderFor(blankLabel));
    }

    // --- helpers ---

    private String exportAsString(LocalDate from, LocalDate to) {
        byte[] bytes = service.exportToCsv(user, from, to);
        // strip the UTF-8 BOM for easier string assertions
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(Arrays.copyOfRange(bytes, 3, bytes.length), StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private Post simplePost(String startDateIso) {
        Post p = new Post();
        p.setId(nextId.incrementAndGet());
        p.setUser(user);
        p.setState(PostState.COMPLETE);
        // Post()'s default constructor sets start = new Date(); override explicitly
        if (startDateIso == null) {
            p.setStart(null);
        } else {
            p.setStart(Date.from(LocalDate.parse(startDateIso).atStartOfDay().toInstant(ZoneOffset.UTC)));
            p.setFinish(Date.from(LocalDateTime.parse(startDateIso + "T01:00:00").toInstant(ZoneOffset.UTC)));
        }
        return p;
    }

    private Category cat(String best, String worst) {
        Category c = new Category();
        c.setBest(best);
        c.setWorst(worst);
        return c;
    }

    private StatDefinition statDef(String name, String label, String emoji, int order) {
        StatDefinition def = new StatDefinition(name, label, emoji, order);
        def.setId(nextId.incrementAndGet());
        return def;
    }
}
