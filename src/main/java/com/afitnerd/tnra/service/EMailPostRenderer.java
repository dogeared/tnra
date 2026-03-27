package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.User;
import org.springframework.stereotype.Service;

@Service("emailPostRenderer")
public class EMailPostRenderer implements PostRenderer {

    @Override
    public String render(Post post) {

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head>\n<meta charset=\"UTF-8\">\n</head>\n<body>");

        User user = post.getUser();
        sb.append("<strong>Post From:</strong> ")
            .append(user.getFirstName()).append(" - ").append(user.getEmail()).append("<br/>\n");
        sb.append("<strong>Post Started:</strong> ").append(PostRenderer.formatDate(post.getStart()));
        if (post.getState() == PostState.COMPLETE) {
            sb.append(", <strong>Post Finished:</strong> ").append(PostRenderer.formatDate(post.getFinish()));
        }
        sb.append("<p/><br/><p/>\n\n");

        Intro intro = post.getIntro();
        sb.append("<h2>Intro:</h2>\n\n");

        sb.append("<h3>WIDWYTK:</h3>\n");
        sb.append(doAppend(intro.getWidwytk())).append("\n\n");

        sb.append("<h3>Kryptonite:</h3>\n");
        sb.append(doAppend(intro.getKryptonite())).append("\n\n");

        sb.append("<h3>What and When:</h3>\n");
        sb.append(doAppend(intro.getWhatAndWhen())).append("\n\n");

        Category personal = post.getPersonal();
        sb.append("<h2>Personal:</h2>\n\n");

        sb.append("<h3>Best:</h3>\n").append(doAppend(personal.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(personal.getWorst())).append("\n\n");

        Category family = post.getFamily();
        sb.append("<h2>Family:</h2>\n\n");

        sb.append("<h3>Best:</h3>\n").append(doAppend(family.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(family.getWorst())).append("\n\n");

        Category work = post.getWork();
        sb.append("<h2>Work:</h2>\n\n");

        sb.append("<h3>Best:</h3>\n").append(doAppend(work.getBest())).append("\n\n");
        sb.append("<h3>Worst:</h3>\n").append(doAppend(work.getWorst())).append("\n\n");

        sb.append("<h2>Stats:</h2>\n\n");
        if (post.getStatValues() == null || post.getStatValues().isEmpty()) {
            sb.append("<p>No stats recorded</p>\n");
        } else {
            sb.append("<p>");
            post.getStatValues().stream()
                .sorted((a, b) -> a.getStatDefinition().getDisplayOrder()
                    .compareTo(b.getStatDefinition().getDisplayOrder()))
                .forEach(sv -> {
                    sb.append("<strong>").append(escapeHtml(sv.getStatDefinition().getLabel())).append(":</strong> ");
                    sb.append(sv.getValue() != null ? sv.getValue() : "not set");
                    sb.append(", ");
                });
            // Remove trailing ", "
            if (sb.length() > 2 && sb.substring(sb.length() - 2).equals(", ")) {
                sb.setLength(sb.length() - 2);
            }
            sb.append("</p>\n");
        }

        sb.append("</body></html>\n");

        return sb.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private StringBuffer doAppend(String toAppend) {
        StringBuffer sb = new StringBuffer();
        return sb
            .append("<p>")
            .append((toAppend != null) ? toAppend : "not set")
            .append("</p>");
    }
}
