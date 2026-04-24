package com.afitnerd.tnra.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionKeyTest {

    @Test
    void gettersAndSetters() {
        EncryptionKey key = new EncryptionKey();
        key.setId(1L);
        key.setEncryptedKey("ENC:somebase64==");
        Date now = new Date();
        key.setCreatedAt(now);

        assertEquals(1L, key.getId());
        assertEquals("ENC:somebase64==", key.getEncryptedKey());
        assertEquals(now, key.getCreatedAt());
    }

    @Test
    void defaultCreatedAtIsSet() {
        EncryptionKey key = new EncryptionKey();
        assertNotNull(key.getCreatedAt());
    }
}
