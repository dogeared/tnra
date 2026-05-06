package com.afitnerd.tnra.model.converter;

import com.afitnerd.tnra.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EncryptedIntegerConverterTest {

    private EncryptionService encryptionService;
    private EncryptedIntegerConverter converter;

    @BeforeEach
    void setUp() {
        encryptionService = mock(EncryptionService.class);
        converter = new EncryptedIntegerConverter();
        ReflectionTestUtils.setField(converter, "encryptionService", encryptionService);
    }

    @Test
    void convertToDatabaseColumn_encryptsIntegerAsString() {
        when(encryptionService.encrypt("42")).thenReturn("ENC:cipher42");
        assertEquals("ENC:cipher42", converter.convertToDatabaseColumn(42));
    }

    @Test
    void convertToDatabaseColumn_null() {
        assertNull(converter.convertToDatabaseColumn(null));
        verifyNoInteractions(encryptionService);
    }

    @Test
    void convertToEntityAttribute_decryptsToInteger() {
        when(encryptionService.decrypt("ENC:cipher42")).thenReturn("42");
        assertEquals(42, converter.convertToEntityAttribute("ENC:cipher42"));
    }

    @Test
    void convertToEntityAttribute_null() {
        assertNull(converter.convertToEntityAttribute(null));
        verifyNoInteractions(encryptionService);
    }

    @Test
    void convertToEntityAttribute_decryptedNullReturnsNull() {
        when(encryptionService.decrypt("ENC:something")).thenReturn(null);
        assertNull(converter.convertToEntityAttribute("ENC:something"));
    }

    @Test
    void roundTrip() {
        when(encryptionService.encrypt("7")).thenReturn("ENC:c7");
        when(encryptionService.decrypt("ENC:c7")).thenReturn("7");
        String db = converter.convertToDatabaseColumn(7);
        assertEquals(7, converter.convertToEntityAttribute(db));
    }
}
