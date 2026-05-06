package com.afitnerd.tnra.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostTokenServiceImplTest {

    private EncryptionService encryptionService;
    private PostTokenServiceImpl postTokenService;

    @BeforeEach
    void setUp() {
        encryptionService = mock(EncryptionService.class);
        postTokenService = new PostTokenServiceImpl(encryptionService);
    }

    @Test
    void encode_stripsPaddingEquals() {
        when(encryptionService.encrypt("42")).thenReturn("ENC:dGVzdA==");
        String token = postTokenService.encode(42L);
        assertEquals("dGVzdA", token, "encode should strip '=' padding");
    }

    @Test
    void encode_replacesPlusWithDashAndSlashWithUnderscore() {
        when(encryptionService.encrypt("42")).thenReturn("ENC:a+b/c+d/");
        String token = postTokenService.encode(42L);
        assertEquals("a-b_c-d_", token, "encode should replace '+' with '-' and '/' with '_'");
    }

    @Test
    void decode_restoresPaddingAndDelegates() {
        when(encryptionService.decrypt("ENC:dGVzdA==")).thenReturn("42");
        Long result = postTokenService.decode("dGVzdA");
        assertEquals(42L, result, "decode should re-pad and return parsed Long");
    }

    @Test
    void decode_reversesUrlSafeSubstitutions() {
        when(encryptionService.decrypt("ENC:a+b/c+d/")).thenReturn("1");
        Long result = postTokenService.decode("a-b_c-d_");
        assertEquals(1L, result, "decode should replace '-' with '+' and '_' with '/'");
    }

    @Test
    void decode_throwsIllegalArgumentExceptionWhenDecryptThrows() {
        when(encryptionService.decrypt(any())).thenThrow(new IllegalStateException("bad crypto"));
        assertThrows(IllegalArgumentException.class, () -> postTokenService.decode("sometoken"),
            "decode should wrap decrypt exceptions in IllegalArgumentException");
    }

    @Test
    void decode_throwsIllegalArgumentExceptionWhenDecryptedIsNotANumber() {
        when(encryptionService.decrypt(any())).thenReturn("not-a-number");
        assertThrows(IllegalArgumentException.class, () -> postTokenService.decode("sometoken"),
            "decode should throw when decrypted string is not a valid Long");
    }

    @Test
    void encode_producesOnlyUrlSafeCharacters() {
        when(encryptionService.encrypt("42")).thenReturn("ENC:a+b/c+d/==");
        String token = postTokenService.encode(42L);
        assertTrue(token.matches("[A-Za-z0-9_-]*"),
            "encode should produce only URL-safe characters: got '" + token + "'");
    }
}
