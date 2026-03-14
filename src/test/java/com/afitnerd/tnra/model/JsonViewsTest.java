package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonViewsTest {

    @Test
    void nestedViewTypesAreInstantiable() {
        JsonViews root = new JsonViews();
        JsonViews.Sparse sparse = new JsonViews.Sparse();
        JsonViews.Full full = new JsonViews.Full();

        assertNotNull(root);
        assertNotNull(sparse);
        assertNotNull(full);
        assertTrue(full instanceof JsonViews.Sparse);
    }
}
