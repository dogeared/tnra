package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityNotificationRendererTest {

    private final ActivityNotificationRenderer renderer =
        new ActivityNotificationRenderer("https://tnra.example.com");

    @Test
    void renderIncludesUserFirstName() {
        Post post = createPost("John", "Doe");
        String html = renderer.render(post);
        assertTrue(html.contains("John"));
    }

    @Test
    void renderDoesNotContainPostContentKeywords() {
        Post post = createPost("Jane", "Doe");
        String html = renderer.render(post);

        // Activity notifications should NOT contain content section markers
        assertFalse(html.contains("Kryptonite"));
        assertFalse(html.contains("What I Don't Want"));
        assertFalse(html.contains("Best"));
        assertFalse(html.contains("Worst"));
        assertFalse(html.contains("Commitments"));
    }

    @Test
    void renderIncludesFinishDate() {
        Post post = createPost("Jane", "Doe");
        post.setFinish(new Date());

        String html = renderer.render(post);
        assertTrue(html.contains(" on "));
    }

    @Test
    void renderHandlesNullFirstName() {
        Post post = createPost(null, "Doe");
        String html = renderer.render(post);
        assertTrue(html.contains("Someone"));
    }

    @Test
    void renderIncludesLoginLink() {
        Post post = createPost("John", "Doe");
        String html = renderer.render(post);
        assertTrue(html.contains("https://tnra.example.com"));
        assertTrue(html.contains("TNRA</a>"));
    }

    @Test
    void renderEscapesHtmlInUserName() {
        Post post = createPost("<script>alert(1)</script>", "Doe");
        String html = renderer.render(post);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    private Post createPost(String firstName, String lastName) {
        User user = new User(firstName, lastName, "test@example.com");
        Post post = new Post();
        post.setUser(user);
        return post;
    }
}
