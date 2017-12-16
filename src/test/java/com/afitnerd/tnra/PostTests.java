package com.afitnerd.tnra;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class PostTests {

    private Post post;
    private Intro intro;
    private Category personal;
    private Category family;
    private Category work;
    private Stats stats;

    @Before
    public void before() {
        post = new Post();
        intro = new Intro();
        personal = new Category();
        family = new Category();
        work = new Category();
        stats = new Stats();
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

        compare(loadFixture("complete"));
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
        return sb.toString();
    }

    private void compare(String fixture) {
        assertEquals(fixture, post.toString());
    }
}
