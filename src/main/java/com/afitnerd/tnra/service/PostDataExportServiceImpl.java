package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.repository.PostRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PostDataExportServiceImpl implements PostDataExportService {

    /** UTF-8 byte-order mark so Excel detects the encoding and renders emoji correctly. */
    static final byte[] UTF8_BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    private static final List<String> BASE_HEADERS = List.of(
        "post_id", "state", "started_at", "finished_at",
        "widwytk", "kryptonite", "what_and_when",
        "personal_best", "personal_worst",
        "family_best", "family_worst",
        "work_best", "work_worst"
    );

    private final PostRepository postRepository;

    public PostDataExportServiceImpl(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public byte[] exportToCsv(User user, LocalDate from, LocalDate to) {
        List<Post> all = postRepository.findByUser(user);
        List<Post> posts = filterByDate(all, from, to);

        // Stat definitions actually used by these posts, ordered by display order
        LinkedHashMap<Long, StatDefinition> statDefs = collectStatDefinitions(posts);
        List<String> headers = new ArrayList<>(BASE_HEADERS);
        for (StatDefinition def : statDefs.values()) {
            headers.add(columnHeaderFor(def));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(UTF8_BOM);
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(headers.toArray(new String[0]))
                     .get())) {

                // Newest first — matches the in-app post selector default
                posts.sort(Comparator.comparing(Post::getStart, Comparator.nullsLast(Comparator.reverseOrder())));
                for (Post post : posts) {
                    printer.printRecord(rowFor(post, statDefs));
                }
                printer.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV", e);
        }
        return out.toByteArray();
    }

    static List<Post> filterByDate(List<Post> posts, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return new ArrayList<>(posts);
        }
        Date fromDate = (from == null) ? null : Date.from(from.atStartOfDay(ZoneOffset.UTC).toInstant());
        // Upper bound is inclusive at end-of-day
        Date toDate = (to == null) ? null : Date.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());

        List<Post> out = new ArrayList<>();
        for (Post p : posts) {
            Date start = p.getStart();
            if (start == null) continue;
            if (fromDate != null && start.before(fromDate)) continue;
            if (toDate != null && !start.before(toDate)) continue;
            out.add(p);
        }
        return out;
    }

    /**
     * Collects every distinct {@link StatDefinition} the posts have a value for,
     * ordered by {@link StatDefinition#getDisplayOrder()} ascending. Definitions
     * with null order sort last. Archived stats are still included if any post
     * has a value — historical data is preserved.
     */
    private LinkedHashMap<Long, StatDefinition> collectStatDefinitions(List<Post> posts) {
        Map<Long, StatDefinition> unique = new LinkedHashMap<>();
        for (Post post : posts) {
            if (post.getStatValues() == null) continue;
            for (PostStatValue v : post.getStatValues()) {
                if (v == null || v.getStatDefinition() == null) continue;
                StatDefinition def = v.getStatDefinition();
                if (def.getId() != null) {
                    unique.putIfAbsent(def.getId(), def);
                }
            }
        }
        LinkedHashMap<Long, StatDefinition> ordered = new LinkedHashMap<>();
        unique.values().stream()
            .sorted(Comparator.comparing(
                StatDefinition::getDisplayOrder,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .forEach(def -> ordered.put(def.getId(), def));
        return ordered;
    }

    /** Emoji prefix + label, falling back to {@code name} when label is blank. */
    static String columnHeaderFor(StatDefinition def) {
        String label = def.getLabel();
        if (label == null || label.isBlank()) {
            label = def.getName();
        }
        String emoji = def.getEmoji();
        if (emoji != null && !emoji.isBlank()) {
            return emoji.trim() + " " + label;
        }
        return label;
    }

    private List<Object> rowFor(Post post, LinkedHashMap<Long, StatDefinition> statDefs) {
        List<Object> row = new ArrayList<>(BASE_HEADERS.size() + statDefs.size());
        row.add(post.getId());
        row.add(post.getState() == null ? "" : post.getState().name());
        row.add(formatDate(post.getStart()));
        row.add(formatDate(post.getFinish()));

        Intro intro = post.getIntro();
        row.add(intro == null ? "" : safe(intro.getWidwytk()));
        row.add(intro == null ? "" : safe(intro.getKryptonite()));
        row.add(intro == null ? "" : safe(intro.getWhatAndWhen()));

        appendCategory(row, post.getPersonal());
        appendCategory(row, post.getFamily());
        appendCategory(row, post.getWork());

        Map<Long, Integer> postValues = new java.util.HashMap<>();
        if (post.getStatValues() != null) {
            for (PostStatValue v : post.getStatValues()) {
                if (v == null || v.getStatDefinition() == null) continue;
                StatDefinition def = v.getStatDefinition();
                if (def.getId() != null) {
                    postValues.put(def.getId(), v.getValue());
                }
            }
        }
        for (Long defId : statDefs.keySet()) {
            Integer value = postValues.get(defId);
            row.add(value == null ? "" : value.toString());
        }
        return row;
    }

    private void appendCategory(List<Object> row, Category category) {
        if (category == null) {
            row.add("");
            row.add("");
        } else {
            row.add(safe(category.getBest()));
            row.add(safe(category.getWorst()));
        }
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return ISO_UTC.format(date.toInstant().atOffset(ZoneOffset.UTC));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
