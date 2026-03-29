package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostGetStatValueTest {

    private StatDefinition createStatDef(Long id, String name) {
        StatDefinition sd = new StatDefinition(name, name, null, 0);
        sd.setId(id);
        return sd;
    }

    private Post createPostWithStatValues() {
        Post post = new Post();
        post.setStatValues(new ArrayList<>());
        return post;
    }

    @Test
    void getStatValue_returnsNullForMissingStat() {
        Post post = createPostWithStatValues();

        // Add a stat value for a different stat name
        StatDefinition sd = createStatDef(1L, "existing_stat");
        post.getStatValues().add(new PostStatValue(post, sd, 5));

        // Query for a stat name that doesn't exist
        assertNull(post.getStatValue("missing_stat"));
    }

    @Test
    void getStatValue_returnsNullWhenValueIsNull() {
        Post post = createPostWithStatValues();

        // Add a stat value where value is explicitly null (the NPE fix scenario)
        StatDefinition sd = createStatDef(1L, "null_value_stat");
        post.getStatValues().add(new PostStatValue(post, sd, null));

        // Should return null without throwing NPE
        assertNull(post.getStatValue("null_value_stat"));
    }

    @Test
    void getStatValue_returnsValueWhenPresent() {
        Post post = createPostWithStatValues();

        StatDefinition sd = createStatDef(1L, "mood");
        post.getStatValues().add(new PostStatValue(post, sd, 8));

        assertEquals(8, post.getStatValue("mood"));
    }
}
