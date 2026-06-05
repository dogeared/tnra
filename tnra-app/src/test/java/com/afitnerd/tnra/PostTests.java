package com.afitnerd.tnra;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostTests {

    private Post post;

    @BeforeEach
    public void before() {
        post = new Post();
    }

    @Test
    public void test_NotNulls() {
        assertNotNull(post.getIntro());
        assertNotNull(post.getPersonal());
        assertNotNull(post.getFamily());
        assertNotNull(post.getWork());
        assertNotNull(post.getStatValues());
        assertTrue(post.getStatValues().isEmpty());
    }

    @Test
    public void test_State() {
        assertEquals(PostState.IN_PROGRESS, post.getState());
    }

    @Test
    public void test_Start() {
        assertNotNull(post.getStart());
    }

    @Test
    public void test_Finish() {
        assertNull(post.getFinish());
    }

    @Test
    public void test_User() {
        User user = new User();
        user.setFirstName("Test");
        post = new Post(user);
        assertEquals("Test", post.getUser().getFirstName());
    }
}
