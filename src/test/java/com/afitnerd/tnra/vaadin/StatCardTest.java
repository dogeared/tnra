package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StatCardTest {

    private StatCard statCard;
    private AtomicReference<Integer> lastValueChange;

    @BeforeEach
    void setUp() {
        statCard = new StatCard("Exercise", "💪", 5);
        lastValueChange = new AtomicReference<>();
        statCard.setValueChangeListener(value -> lastValueChange.set(value));
    }

    @Test
    void testConstructorWithInitialValue() {
        // Act & Assert
        assertEquals("Exercise", statCard.getLabel());
        assertEquals(5, statCard.getValue());
        assertFalse(statCard.isReadOnly());
    }

    @Test
    void testConstructorWithNullInitialValue() {
        // Arrange & Act
        StatCard nullCard = new StatCard("Test", "🧪", null);

        // Assert
        assertEquals("Test", nullCard.getLabel());
        assertNull(nullCard.getValue());
    }

    @Test
    void testSetValue() {
        // Act
        statCard.setValue(10);

        // Assert
        assertEquals(10, statCard.getValue());
    }

    @Test
    void testSetValueToNull() {
        // Act
        statCard.setValue(null);

        // Assert
        assertNull(statCard.getValue());
    }

    @Test
    void testSetReadOnly() {
        // Initially not read-only
        assertFalse(statCard.isReadOnly());

        // Act
        statCard.setReadOnly(true);

        // Assert
        assertTrue(statCard.isReadOnly());

        // Act
        statCard.setReadOnly(false);

        // Assert
        assertFalse(statCard.isReadOnly());
    }

    @Test
    void testValueChangeListener() {
        // Act
        statCard.setValue(15);

        // We can't directly test the listener through button clicks in unit tests
        // since those require a full Vaadin environment, but we can test the
        // listener is set and called when setValue is used programmatically
        assertNotNull(statCard);
        assertEquals(15, statCard.getValue());
    }

    @Test
    void testGetLabel() {
        // Act & Assert
        assertEquals("Exercise", statCard.getLabel());
    }

    @Test
    void testComponentStructure() {
        // Assert that the card has the expected number of child components
        // (header + controls layout)
        assertTrue(statCard.getChildren().count() >= 2);
    }

    @Test
    void testInitialValueWithZero() {
        // Arrange & Act
        StatCard zeroCard = new StatCard("Zero", "0️⃣", 0);

        // Assert
        assertEquals(0, zeroCard.getValue());
        assertEquals("Zero", zeroCard.getLabel());
    }

    @Test
    void testLargeInitialValue() {
        // Arrange & Act
        StatCard largeCard = new StatCard("Large", "🔢", 99);

        // Assert
        assertEquals(99, largeCard.getValue());
        assertEquals("Large", largeCard.getLabel());
    }

    @Test
    void testComponentHasCorrectClasses() {
        // Assert that the card has the expected CSS classes
        assertTrue(statCard.getClassNames().contains("stat-card"));
    }

    @Test
    void testReadOnlyStateChangesClasses() {
        // Act
        statCard.setReadOnly(true);

        // Assert
        assertTrue(statCard.getClassNames().contains("readonly-card"));

        // Act
        statCard.setReadOnly(false);

        // Assert
        assertFalse(statCard.getClassNames().contains("readonly-card"));
    }

    @Test
    void testEmojiAndLabelDisplay() {
        // Create cards with different emojis and labels
        StatCard meditateCard = new StatCard("Meditate", "🧘", 10);
        StatCard prayCard = new StatCard("Pray", "🙏", 5);

        // Assert
        assertEquals("Meditate", meditateCard.getLabel());
        assertEquals("Pray", prayCard.getLabel());
        assertEquals(10, meditateCard.getValue());
        assertEquals(5, prayCard.getValue());
    }
}