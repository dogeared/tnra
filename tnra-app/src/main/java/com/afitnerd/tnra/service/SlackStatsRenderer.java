package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Renders a {@link Post}'s stat values as Slack mrkdwn. Each line:
 * <pre>{emoji} {label}: {value}</pre>
 * Archived stats and entries with null values are skipped. Ordering follows
 * {@link StatDefinition#getDisplayOrder()}.
 */
@Component
public class SlackStatsRenderer {

    public String render(Post post) {
        List<PostStatValue> values = post.getStatValues();
        if (values == null || values.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("*Stats*\n");
        boolean any = false;
        List<PostStatValue> ordered = values.stream()
            .filter(v -> v != null && v.getStatDefinition() != null)
            .filter(v -> !Boolean.TRUE.equals(v.getStatDefinition().getArchived()))
            .filter(v -> v.getValue() != null)
            .sorted(Comparator.comparing(
                v -> v.getStatDefinition().getDisplayOrder() == null ? Integer.MAX_VALUE : v.getStatDefinition().getDisplayOrder()))
            .toList();

        for (PostStatValue v : ordered) {
            StatDefinition def = v.getStatDefinition();
            String emoji = def.getEmoji() == null ? "" : def.getEmoji().trim();
            String label = def.getLabel() == null ? def.getName() : def.getLabel();
            if (!emoji.isEmpty()) {
                sb.append(emoji).append(" ");
            }
            sb.append("*").append(label).append(":* ").append(v.getValue()).append("\n");
            any = true;
        }

        return any ? sb.toString().stripTrailing() : "";
    }
}
