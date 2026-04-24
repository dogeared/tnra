package com.afitnerd.tnra.model.converter;

import com.afitnerd.tnra.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EncryptedStringConverterTest {

    private EncryptionService encryptionService;
    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        encryptionService = mock(EncryptionService.class);
        converter = new EncryptedStringConverter();
        ReflectionTestUtils.setField(converter, "encryptionService", encryptionService);
    }

    @Test
    void convertToDatabaseColumn_callsEncrypt() {
        when(encryptionService.encrypt("hello")).thenReturn("ENC:ciphertext");
        assertEquals("ENC:ciphertext", converter.convertToDatabaseColumn("hello"));
    }

    @Test
    void convertToDatabaseColumn_null() {
        when(encryptionService.encrypt(null)).thenReturn(null);
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttribute_callsDecrypt() {
        when(encryptionService.decrypt("ENC:ciphertext")).thenReturn("hello");
        assertEquals("hello", converter.convertToEntityAttribute("ENC:ciphertext"));
    }

    @Test
    void convertToEntityAttribute_null() {
        when(encryptionService.decrypt(null)).thenReturn(null);
        assertNull(converter.convertToEntityAttribute(null));
    }
}
