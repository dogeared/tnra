package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatDefinitionTest {

    @Test
    void settersAndGettersRoundTrip() {
        StatDefinition sd = new StatDefinition();
        sd.setName("exercise");
        sd.setLabel("Exercise");
        sd.setEmoji("💪");
        sd.setStatType(StatDefinition.StatType.BOOLEAN);

        assertEquals("exercise", sd.getName());
        assertEquals("Exercise", sd.getLabel());
        assertEquals("💪", sd.getEmoji());
        assertEquals(StatDefinition.StatType.BOOLEAN, sd.getStatType());
    }

    @Test
    void defaultStatTypeIsNumeric() {
        StatDefinition sd = new StatDefinition();
        assertEquals(StatDefinition.StatType.NUMERIC, sd.getStatType());
    }

    @Test
    void parameterizedConstructorSetsFields() {
        StatDefinition sd = new StatDefinition("pray", "Pray", "🙏", 3);
        assertEquals("pray", sd.getName());
        assertEquals("Pray", sd.getLabel());
        assertEquals("🙏", sd.getEmoji());
        assertEquals(3, sd.getDisplayOrder());
    }

    @Test
    void setStatTypeTextVariant() {
        StatDefinition sd = new StatDefinition();
        sd.setStatType(StatDefinition.StatType.TEXT);
        assertEquals(StatDefinition.StatType.TEXT, sd.getStatType());
    }
}
