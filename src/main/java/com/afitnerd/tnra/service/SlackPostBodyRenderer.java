package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import org.springframework.stereotype.Component;

/**
 * Renders the textual body of a {@link Post} as Slack mrkdwn, suitable for
 * appending to an activity notification. Skips fields that are null or blank
 * so a partially-filled post still renders cleanly.
 */
@Component
public class SlackPostBodyRenderer {

    public String render(Post post) {
        StringBuilder sb = new StringBuilder();

        Intro intro = post.getIntro();
        if (intro != null && hasAnyIntro(intro)) {
            sb.append("*Intro*\n");
            appendField(sb, "What I Don't Want You To Know", intro.getWidwytk());
            appendField(sb, "Kryptonite", intro.getKryptonite());
            appendField(sb, "What and When", intro.getWhatAndWhen());
        }

        appendCategory(sb, "Personal", post.getPersonal());
        appendCategory(sb, "Family", post.getFamily());
        appendCategory(sb, "Work", post.getWork());

        return sb.toString().stripTrailing();
    }

    private void appendCategory(StringBuilder sb, String title, Category category) {
        if (category == null || (isBlank(category.getBest()) && isBlank(category.getWorst()))) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append("*").append(title).append("*\n");
        appendField(sb, "Best", category.getBest());
        appendField(sb, "Worst", category.getWorst());
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (isBlank(value)) {
            return;
        }
        sb.append("> *").append(label).append(":* ").append(escape(value)).append("\n");
    }

    private boolean hasAnyIntro(Intro intro) {
        return !isBlank(intro.getWidwytk())
            || !isBlank(intro.getKryptonite())
            || !isBlank(intro.getWhatAndWhen());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Slack mrkdwn-safe escape: only HTML-style chars need encoding in text payloads. */
    private String escape(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
