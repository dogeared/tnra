package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostStatValueTest {

    @Test
    void defaultConstructorCreatesEmptyInstance() {
        PostStatValue psv = new PostStatValue();
        assertNull(psv.getId());
        assertNull(psv.getPost());
        assertNull(psv.getStatDefinition());
        assertNull(psv.getValue());
    }

    @Test
    void parameterizedConstructorSetsFields() {
        Post post = new Post();
        StatDefinition statDef = new StatDefinition("exercise", "Exercise", "💪", 0);
        PostStatValue psv = new PostStatValue(post, statDef, 5);

        assertNotNull(psv.getPost());
        assertNotNull(psv.getStatDefinition());
        assertEquals(5, psv.getValue());
    }

    @Test
    void settersAndGettersWork() {
        PostStatValue psv = new PostStatValue();
        Post post = new Post();
        StatDefinition statDef = new StatDefinition("med", "Meditate", "🧘", 1);

        psv.setId(42L);
        psv.setPost(post);
        psv.setStatDefinition(statDef);
        psv.setValue(7);

        assertEquals(42L, psv.getId());
        assertEquals(post, psv.getPost());
        assertEquals(statDef, psv.getStatDefinition());
        assertEquals(7, psv.getValue());
    }
}
