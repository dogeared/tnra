package com.afitnerd.tnra;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PostTests {

    private Post post;
    private Intro intro;
    private Category personal;
    private Category family;
    private Category work;
    private Stats stats;

    @BeforeEach
    public void before() {
        post = new Post();
        intro = new Intro();
        personal = new Category();
        family = new Category();
        work = new Category();
        stats = new Stats();
    }

    @Test
    public void test_NotNulls() {
        assertNotNull(post.getIntro());
        assertNotNull(post.getPersonal());
        assertNotNull(post.getFamily());
        assertNotNull(post.getWork());
        assertNotNull(post.getStats());
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
        user.setSlackUserId("slack");
        post = new Post(user);
        assertEquals("slack", post.getUser().getSlackUserId());
    }
}
