package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("activityNotificationRenderer")
public class ActivityNotificationRenderer implements PostRenderer {

    private final String baseUrl;

    public ActivityNotificationRenderer(
        @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String render(Post post) {
        User user = post.getUser();
        String name = (user != null && user.getFirstName() != null)
            ? escapeHtml(user.getFirstName())
            : "Someone";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset=\"UTF-8\"></head><body>");
        sb.append("<h2>TNRA Activity Update</h2>");
        sb.append("<p><strong>").append(name).append("</strong>");
        sb.append(" posted their weekly update");
        if (post.getFinish() != null) {
            sb.append(" on ").append(PostRenderer.formatDate(post.getFinish()));
        }
        sb.append(".</p>");
        sb.append("<p>Log in to <a href=\"").append(escapeHtml(baseUrl));
        sb.append("\">TNRA</a> to view the full post.</p>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
