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
        assertNotNull(post.getStatValues());
    }

    @Test
    void toStringRendersUnsetSectionsClearly() {
        Post post = new Post();
        post.setIntro(null);
        post.setPersonal(null);
        post.setFamily(null);
        post.setWork(null);

        String rendered = post.toString();

        assertTrue(rendered.contains("*Intro not set*"));
        assertTrue(rendered.contains("*Personal not set*"));
        assertTrue(rendered.contains("*Family not set*"));
        assertTrue(rendered.contains("*Work not set*"));
        assertTrue(rendered.contains("*Stats not set*"));
    }

    @Test
    void toStringFormatsConfiguredSectionsAndStats() {
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

        StatDefinition exerciseDef = new StatDefinition("exercise", "Exercise", "💪", 0);
        exerciseDef.setId(1L);
        StatDefinition gtgDef = new StatDefinition("gtg", "GTG", "👥", 1);
        gtgDef.setId(2L);

        post.setStatValue(exerciseDef, 5);
        post.setStatValue(gtgDef, 3);

        String rendered = post.toString();

        assertTrue(rendered.contains("line1\n\tline2"));
        assertTrue(rendered.contains("\t*Best:* best"));
        assertTrue(rendered.contains("\t*Worst:* worst"));
        assertTrue(rendered.contains("*Exercise:* 5"));
        assertTrue(rendered.contains("*GTG:* 3"));
    }
}
