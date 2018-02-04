package com.afitnerd.tnra.slack;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.PostRenderer;
import com.afitnerd.tnra.service.SlackPostRenderer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        compare(loadFixture("empty"));
    }

    @Test
    public void testToString_Wid() {
        intro.setWidwytk("wid");
        post.setIntro(intro);
        compare(loadFixture("wid"));
    }

    @Test
    public void testToString_Kry() {
        intro.setKryptonite("kry");
        post.setIntro(intro);
        compare(loadFixture("kry"));
    }

    @Test
    public void testToString_Wha() {
        intro.setWhatAndWhen("wha");
        post.setIntro(intro);
        compare(loadFixture("wha"));
    }

    @Test
    public void testToString_Per_Bes() {
        personal.setBest("bes");
        post.setPersonal(personal);
        compare(loadFixture("per_bes"));
    }

    @Test
    public void testToString_Per_Wor() {
        personal.setWorst("wor");
        post.setPersonal(personal);
        compare(loadFixture("per_wor"));
    }

    @Test
    public void testToString_Fam_Bes() {
        family.setBest("bes");
        post.setFamily(family);
        compare(loadFixture("fam_bes"));
    }

    @Test
    public void testToString_Fam_Wor() {
        family.setWorst("wor");
        post.setFamily(family);
        compare(loadFixture("fam_wor"));
    }

    @Test
    public void testToString_Wor_Bes() {
        work.setBest("bes");
        post.setWork(work);
        compare(loadFixture("wor_bes"));
    }

    @Test
    public void testToString_Wor_Wor() {
        work.setWorst("wor");
        post.setWork(work);
        compare(loadFixture("wor_wor"));
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
        compare(loadFixture("stats"));
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

        compare(loadFixture("complete"));
    }

    private String loadFixture(String filename) {

        StringBuffer sb = new StringBuffer();
        try {
            ClassPathResource resource = new ClassPathResource("/fixtures/" + filename + ".txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String ret = sb.toString();

        if (post.getStart() != null) {
            ret = ret.replace("{{start_date}}", PostRenderer.formatDate(post.getStart()));
        }

        if (post.getFinish() != null) {
            ret = ret.replace("{{finish_date}}", PostRenderer.formatDate(post.getFinish()));
        }

        return ret;
    }

    private void compare(String fixture) {
        assertEquals(fixture, slackPostRenderer.render(post));
    }
}
