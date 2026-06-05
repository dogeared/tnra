package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserMissingFieldsTest {

    @Test
    void textEmailSuffixRoundTrip() {
        User user = new User();
        user.setTextEmailSuffix("@example.com");
        assertEquals("@example.com", user.getTextEmailSuffix());
    }

    @Test
    void postsRoundTrip() {
        User user = new User();
        Post p = new Post();
        user.setPosts(List.of(p));
        assertEquals(1, user.getPosts().size());
        assertSame(p, user.getPosts().get(0));
    }

    @Test
    void slackUsernameRoundTrip() {
        User user = new User();
        user.setSlackUsername("johndoe");
        assertEquals("johndoe", user.getSlackUsername());
    }

    @Test
    void slackUserIdRoundTrip() {
        User user = new User();
        user.setSlackUserId("U12345");
        assertEquals("U12345", user.getSlackUserId());
    }
}
