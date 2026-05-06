package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonalStatDefinitionTest {

    @Test
    void defaultConstructorCreatesEmptyInstance() {
        PersonalStatDefinition psd = new PersonalStatDefinition();
        assertNull(psd.getUser());
        assertNull(psd.getName());
    }

    @Test
    void parameterizedConstructorSetsAllFields() {
        User user = new User("Test", "User", "test@example.com");
        PersonalStatDefinition psd = new PersonalStatDefinition("guitar", "Guitar Practice", "🎸", 0, user);

        assertEquals("guitar", psd.getName());
        assertEquals("Guitar Practice", psd.getLabel());
        assertEquals("🎸", psd.getEmoji());
        assertEquals(0, psd.getDisplayOrder());
        assertEquals(user, psd.getUser());
    }

    @Test
    void setUserUpdatesUser() {
        PersonalStatDefinition psd = new PersonalStatDefinition();
        User user = new User("Test", "User", "test@example.com");
        psd.setUser(user);
        assertEquals(user, psd.getUser());
    }
}
