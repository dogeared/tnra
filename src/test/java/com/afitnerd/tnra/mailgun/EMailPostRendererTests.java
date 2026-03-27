package com.afitnerd.tnra.mailgun;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.EMailPostRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EMailPostRendererTests {

    private Post post;
    private EMailPostRenderer eMailPostRenderer;

    @BeforeEach
    public void before() {
        post = new Post();

        User user = new User();
        user.setFirstName("Timmy");
        user.setLastName("Timbob");
        user.setEmail("timmy@timmyco.com");
        post.setUser(user);

        eMailPostRenderer = new EMailPostRenderer();
    }

    @Test
    public void testRender_Complete() {
        Intro intro = new Intro();
        intro.setWidwytk("wid");
        intro.setKryptonite("kry");
        intro.setWhatAndWhen("wha");
        post.setIntro(intro);

        Category personal = new Category();
        personal.setBest("bes");
        personal.setWorst("wor");
        post.setPersonal(personal);

        Category family = new Category();
        family.setBest("bes");
        family.setWorst("wor");
        post.setFamily(family);

        Category work = new Category();
        work.setBest("bes");
        work.setWorst("wor");
        post.setWork(work);

        StatDefinition exerciseDef = new StatDefinition("exercise", "Exercise", "💪", 0);
        exerciseDef.setId(1L);
        StatDefinition gtgDef = new StatDefinition("gtg", "GTG", "👥", 1);
        gtgDef.setId(2L);
        post.setStatValue(exerciseDef, 1);
        post.setStatValue(gtgDef, 2);

        post.setFinish(new Date());
        post.setState(PostState.COMPLETE);

        String rendered = eMailPostRenderer.render(post);

        assertTrue(rendered.contains("Timmy"));
        assertTrue(rendered.contains("timmy@timmyco.com"));
        assertTrue(rendered.contains("WIDWYTK"));
        assertTrue(rendered.contains("wid"));
        assertTrue(rendered.contains("Kryptonite"));
        assertTrue(rendered.contains("kry"));
        assertTrue(rendered.contains("<strong>Exercise:</strong> 1"));
        assertTrue(rendered.contains("<strong>GTG:</strong> 2"));
        assertTrue(rendered.contains("Post Finished"));
    }

    @Test
    public void testRender_NoStats() {
        Intro intro = new Intro();
        intro.setWidwytk("test");
        intro.setKryptonite("test");
        intro.setWhatAndWhen("test");
        post.setIntro(intro);

        String rendered = eMailPostRenderer.render(post);

        assertTrue(rendered.contains("No stats recorded"));
    }
}
