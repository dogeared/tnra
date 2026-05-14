package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlackPostBodyRendererTest {

    private SlackPostBodyRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new SlackPostBodyRenderer();
    }

    @Test
    void rendersAllSectionsWhenFullyPopulated() {
        Post post = new Post();
        Intro intro = new Intro();
        intro.setWidwytk("hidden");
        intro.setKryptonite("weak");
        intro.setWhatAndWhen("plan");
        post.setIntro(intro);
        post.setPersonal(category("p-best", "p-worst"));
        post.setFamily(category("f-best", "f-worst"));
        post.setWork(category("w-best", "w-worst"));

        String out = renderer.render(post);

        assertTrue(out.contains("*Intro*"));
        assertTrue(out.contains("*What I Don't Want You To Know:* hidden"));
        assertTrue(out.contains("*Kryptonite:* weak"));
        assertTrue(out.contains("*What and When:* plan"));
        assertTrue(out.contains("*Personal*"));
        assertTrue(out.contains("*Best:* p-best"));
        assertTrue(out.contains("*Worst:* p-worst"));
        assertTrue(out.contains("*Family*"));
        assertTrue(out.contains("*Work*"));
    }

    @Test
    void skipsBlankFields() {
        Post post = new Post();
        Intro intro = new Intro();
        intro.setWidwytk("");
        intro.setKryptonite("only this");
        intro.setWhatAndWhen("   ");
        post.setIntro(intro);

        String out = renderer.render(post);

        assertTrue(out.contains("*Kryptonite:* only this"));
        assertFalse(out.contains("What I Don't Want You To Know"));
        assertFalse(out.contains("What and When"));
    }

    @Test
    void skipsCategoriesWhereBothHalvesEmpty() {
        Post post = new Post();
        Category empty = new Category();
        empty.setBest("");
        empty.setWorst(null);
        post.setPersonal(empty);
        post.setFamily(category("family-best", null));

        String out = renderer.render(post);

        assertFalse(out.contains("*Personal*"), "Empty Personal section must be skipped");
        assertTrue(out.contains("*Family*"));
        assertTrue(out.contains("*Best:* family-best"));
    }

    @Test
    void returnsEmptyStringForEmptyPost() {
        Post post = new Post();
        assertEquals("", renderer.render(post));
    }

    @Test
    void escapesAngleBracketsAndAmpersands() {
        Post post = new Post();
        Intro intro = new Intro();
        intro.setWidwytk("<script>alert('x')</script> & more");
        post.setIntro(intro);

        String out = renderer.render(post);

        assertTrue(out.contains("&lt;script&gt;"));
        assertTrue(out.contains("&amp; more"));
        assertFalse(out.contains("<script>"));
    }

    @Test
    void usesBlockquoteForFieldValues() {
        Post post = new Post();
        Intro intro = new Intro();
        intro.setWidwytk("hidden");
        post.setIntro(intro);

        String out = renderer.render(post);
        assertTrue(out.contains("> *What I Don't Want You To Know:*"),
            "Field values must use Slack blockquote prefix");
    }

    private Category category(String best, String worst) {
        Category c = new Category();
        c.setBest(best);
        c.setWorst(worst);
        return c;
    }
}
