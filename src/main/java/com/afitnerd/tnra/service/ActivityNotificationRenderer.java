package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service("activityNotificationRenderer")
public class ActivityNotificationRenderer implements PostRenderer {

    private final PostTokenService postTokenService;
    private final String baseUrl;

    public ActivityNotificationRenderer(
        PostTokenService postTokenService,
        @Value("${tnra.app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.postTokenService = postTokenService;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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
        if (post.getId() != null) {
            String postLink = baseUrl + "/posts/" + postTokenService.encode(post.getId());
            sb.append("<p><a href=\"").append(escapeHtml(postLink));
            sb.append("\">View post</a></p>");
        } else {
            sb.append("<p><a href=\"").append(escapeHtml(baseUrl));
            sb.append("\">View on TNRA</a></p>");
        }
        sb.append("</body></html>");

        return sb.toString();
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
