package com.afitnerd.tnra.slack;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.SlackPostRenderer;
import com.afitnerd.tnra.utils.FixtureUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class SlackPostRendererTests {

    private Post post;
    private Intro intro;
    private Category personal;
    private Category family;
    private Category work;
    private Stats stats;

    private SlackPostRenderer slackPostRenderer;

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

        slackPostRenderer = new SlackPostRenderer();
    }


    @Test
    public void testToString_Empty() {
        compare(FixtureUtils.loadFixture("slack", "empty", post));
    }

    @Test
    public void testToString_Wid() {
        intro.setWidwytk("wid");
        post.setIntro(intro);
        compare(FixtureUtils.loadFixture("slack", "wid", post));
    }

    @Test
    public void testToString_Kry() {
        intro.setKryptonite("kry");
        post.setIntro(intro);
        compare(FixtureUtils.loadFixture("slack", "kry", post));
    }

    @Test
    public void testToString_Wha() {
        intro.setWhatAndWhen("wha");
        post.setIntro(intro);
        compare(FixtureUtils.loadFixture("slack", "wha", post));
    }

    @Test
    public void testToString_Per_Bes() {
        personal.setBest("bes");
        post.setPersonal(personal);
        compare(FixtureUtils.loadFixture("slack", "per_bes", post));
    }

    @Test
    public void testToString_Per_Wor() {
        personal.setWorst("wor");
        post.setPersonal(personal);
        compare(FixtureUtils.loadFixture("slack", "per_wor", post));
    }

    @Test
    public void testToString_Fam_Bes() {
        family.setBest("bes");
        post.setFamily(family);
        compare(FixtureUtils.loadFixture("slack", "fam_bes", post));
    }

    @Test
    public void testToString_Fam_Wor() {
        family.setWorst("wor");
        post.setFamily(family);
        compare(FixtureUtils.loadFixture("slack", "fam_wor", post));
    }

    @Test
    public void testToString_Wor_Bes() {
        work.setBest("bes");
        post.setWork(work);
        compare(FixtureUtils.loadFixture("slack", "wor_bes", post));
    }

    @Test
    public void testToString_Wor_Wor() {
        work.setWorst("wor");
        post.setWork(work);
        compare(FixtureUtils.loadFixture("slack", "wor_wor", post));
    }

    @Test
    public void testToString_Stats() {
        stats.setExercise(1);
        stats.setGtg(2);
        stats.setMeditate(3);
        stats.setMeetings(4);
        stats.setPray(5);
        stats.setRead(6);
        stats.setSponsor(7);
        post.setStats(stats);
        compare(FixtureUtils.loadFixture("slack", "stats", post));
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

        compare(FixtureUtils.loadFixture("slack", "complete", post));
    }

    private void compare(String fixture) {
        assertEquals(fixture, slackPostRenderer.render(post));
    }
}
