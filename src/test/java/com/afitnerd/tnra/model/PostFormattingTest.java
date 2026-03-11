package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostFormattingTest {

    @Test
    void defaultConstructorStartsInProgressWithStartTime() {
        Post post = new Post();

        assertEquals(PostState.IN_PROGRESS, post.getState());
        assertNotNull(post.getStart());
    }

    @Test
    void gettersLazilyCreateNestedStructures() {
        Post post = new Post();

        assertNotNull(post.getIntro());
        assertNotNull(post.getPersonal());
        assertNotNull(post.getFamily());
        assertNotNull(post.getWork());
        assertNotNull(post.getStats());
    }

    @Test
    void toStringRendersUnsetSectionsClearly() {
        Post post = new Post();
        post.setIntro(null);
        post.setPersonal(null);
        post.setFamily(null);
        post.setWork(null);
        post.setStats(null);

        String rendered = post.toString();

        assertTrue(rendered.contains("*Intro not set*"));
        assertTrue(rendered.contains("*Personal not set*"));
        assertTrue(rendered.contains("*Family not set*"));
        assertTrue(rendered.contains("*Work not set*"));
        assertTrue(rendered.contains("*Stats not set*"));
    }

    @Test
    void toStringFormatsConfiguredSectionsAndNormalizesLineBreaks() {
        Post post = new Post();
        Intro intro = new Intro();
        intro.setWidwytk("line1\nline2");
        intro.setKryptonite("kryptonite");
        intro.setWhatAndWhen("what");
        post.setIntro(intro);

        Category personal = new Category();
        personal.setBest("best");
        personal.setWorst("worst");
        post.setPersonal(personal);

        Category family = new Category();
        family.setBest("family best");
        family.setWorst("family worst");
        post.setFamily(family);

        Category work = new Category();
        work.setBest("work best");
        work.setWorst("work worst");
        post.setWork(work);

        Stats stats = new Stats();
        stats.setExercise(1);
        stats.setGtg(2);
        stats.setMeditate(3);
        stats.setMeetings(4);
        stats.setPray(5);
        stats.setRead(6);
        stats.setSponsor(7);
        post.setStats(stats);

        String rendered = post.toString();

        assertTrue(rendered.contains("line1\n\tline2"));
        assertTrue(rendered.contains("\t*Best:* best"));
        assertTrue(rendered.contains("\t*Worst:* worst"));
        assertTrue(rendered.contains("*exercise:* 1, *gtg:* 2, *meditate:* 3"));
        assertTrue(rendered.contains("*sponsor:* 7"));
    }
}
