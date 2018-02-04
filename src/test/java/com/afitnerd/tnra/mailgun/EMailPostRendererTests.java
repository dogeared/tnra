package com.afitnerd.tnra.mailgun;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.EMailPostRenderer;
import com.afitnerd.tnra.utils.FixtureUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class EMailPostRendererTests {

    private Post post;
    private Intro intro;
    private Category personal;
    private Category family;
    private Category work;
    private Stats stats;

    private EMailPostRenderer eMailPostRenderer;

    @Before
    public void before() {
        post = new Post();
        intro = new Intro();
        personal = new Category();
        family = new Category();
        work = new Category();
        stats = new Stats();

        User user = new User();
        user.setFirstName("Timmy");
        user.setLastName("Timbob");
        user.setEmail("timmy@timmyco.com");
        user.setSlackUsername("timmy");
        post.setUser(user);

        eMailPostRenderer = new EMailPostRenderer();
    }

    @Test
    public void testToString_Complete() {

        intro.setWidwytk("wid");
        intro.setKryptonite("kry");
        intro.setWhatAndWhen("wha");
        post.setIntro(intro);

        personal.setBest("bes");
        personal.setWorst("wor");
        post.setPersonal(personal);

        family.setBest("bes");
        family.setWorst("wor");
        post.setFamily(family);

        work.setBest("bes");
        work.setWorst("wor");
        post.setWork(work);

        stats.setExercise(1);
        stats.setGtg(2);
        stats.setMeditate(3);
        stats.setMeetings(4);
        stats.setPray(5);
        stats.setRead(6);
        stats.setSponsor(7);
        post.setStats(stats);
        post.setFinish(new Date());
        post.setState(PostState.COMPLETE);

        compare(FixtureUtils.loadFixture("email", "complete", post));
    }

    private void compare(String fixture) {
        assertEquals(fixture, eMailPostRenderer.render(post));
    }
}
