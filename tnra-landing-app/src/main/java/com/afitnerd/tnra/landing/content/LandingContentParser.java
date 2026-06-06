package com.afitnerd.tnra.landing.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the landing app's "modified Markdown" content files into an ordered list
 * of {@link Block}s. Everything is plain Markdown except for container directives
 * fenced by a line {@code :::<type>} (open) and a bare {@code :::} (close):
 *
 * <pre>
 *   :::cards squares        cards/squares grid (see {@link #parseCards})
 *   :::hero                 hero section (Markdown headline + sub + link CTA)
 *   :::cta                  a single link rendered as a primary button
 *   :::form                 the live Request Access form (key: value fields)
 * </pre>
 *
 * See content/README.md for the full format reference. The parser is pure (no
 * Vaadin, no IO) so it can be unit tested directly.
 */
public final class LandingContentParser {

    private LandingContentParser() {
    }

    /** A top-level content block: plain {@code markdown} or a custom directive. */
    public record Block(String type, String args, String body) {
    }

    /** One card inside a {@code :::cards} block. */
    public record Card(String icon, String title, boolean featured, String body) {
    }

    /** The parsed contents of a {@code :::cards} block: intro Markdown + cards. */
    public record CardsBlock(String intro, List<Card> cards) {
    }

    public static List<Block> parse(String content) {
        List<Block> blocks = new ArrayList<>();
        if (content == null) {
            return blocks;
        }
        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);

        List<String> text = new ArrayList<>();
        int i = 0;
        while (i < lines.length) {
            String stripped = lines[i].strip();
            String[] open = openFence(stripped);
            if (open != null) {
                flushMarkdown(text, blocks);
                List<String> body = new ArrayList<>();
                i++;
                while (i < lines.length && !lines[i].strip().equals(":::")) {
                    body.add(lines[i]);
                    i++;
                }
                blocks.add(new Block(open[0], open[1].isBlank() ? null : open[1].strip(), String.join("\n", body)));
                i++; // skip the closing :::
            } else {
                text.add(lines[i]);
                i++;
            }
        }
        flushMarkdown(text, blocks);
        return blocks;
    }

    /** @return {type, args} for an opening fence line, or null if not one. */
    private static String[] openFence(String stripped) {
        if (!stripped.startsWith(":::") || stripped.equals(":::")) {
            return null;
        }
        String rest = stripped.substring(3).strip();
        if (rest.isEmpty()) {
            return null;
        }
        int space = rest.indexOf(' ');
        if (space < 0) {
            return new String[] {rest, ""};
        }
        return new String[] {rest.substring(0, space), rest.substring(space + 1)};
    }

    private static void flushMarkdown(List<String> text, List<Block> blocks) {
        String joined = String.join("\n", text).strip();
        if (!joined.isEmpty()) {
            blocks.add(new Block("markdown", null, joined));
        }
        text.clear();
    }

    /**
     * Parse the body of a {@code :::cards} block. Lines before the first {@code :: }
     * card heading are the section intro (Markdown). Each {@code :: heading} starts a
     * card; a leading emoji becomes its icon and a trailing {@code {featured}} marks it.
     */
    public static CardsBlock parseCards(String body) {
        List<String> intro = new ArrayList<>();
        List<Card> cards = new ArrayList<>();
        String[] lines = body == null ? new String[0] : body.split("\n", -1);

        String heading = null;
        List<String> cardBody = new ArrayList<>();
        for (String line : lines) {
            String stripped = line.strip();
            if (stripped.equals("::") || stripped.startsWith(":: ")) {
                if (heading != null) {
                    cards.add(toCard(heading, cardBody));
                }
                heading = stripped.length() > 2 ? stripped.substring(2).strip() : "";
                cardBody = new ArrayList<>();
            } else if (heading == null) {
                intro.add(line);
            } else {
                cardBody.add(line);
            }
        }
        if (heading != null) {
            cards.add(toCard(heading, cardBody));
        }
        return new CardsBlock(String.join("\n", intro).strip(), cards);
    }

    private static Card toCard(String heading, List<String> body) {
        boolean featured = false;
        String title = heading;
        if (title.endsWith("{featured}")) {
            featured = true;
            title = title.substring(0, title.length() - "{featured}".length()).strip();
        }
        String icon = null;
        int space = title.indexOf(' ');
        if (space > 0) {
            String first = title.substring(0, space);
            // Treat a leading non-ASCII/non-letter token (e.g. an emoji) as the icon.
            if (first.codePointAt(0) > 0x2000) {
                icon = first;
                title = title.substring(space + 1).strip();
            }
        }
        return new Card(icon, title, featured, String.join("\n", body).strip());
    }

    /** Parse {@code key: value} lines (used by the {@code :::form} block). */
    public static Map<String, String> parseFields(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (body == null) {
            return fields;
        }
        for (String line : body.split("\n", -1)) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                fields.put(line.substring(0, colon).strip(), line.substring(colon + 1).strip());
            }
        }
        return fields;
    }
}
