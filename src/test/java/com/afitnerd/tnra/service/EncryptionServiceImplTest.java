package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.EncryptionKey;
import com.afitnerd.tnra.repository.EncryptionKeyRepository;
import com.afitnerd.tnra.util.AesGcm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EncryptionServiceImplTest {

    private static final String TEST_MASTER_KEY_BASE64 = Base64.getEncoder().encodeToString(new byte[32]);

    private EncryptionKeyRepository keyRepository;
    private EncryptionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        keyRepository = mock(EncryptionKeyRepository.class);

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        byte[] testDek = keyGen.generateKey().getEncoded();
        byte[] masterKey = Base64.getDecoder().decode(TEST_MASTER_KEY_BASE64);
        byte[] encryptedDek = AesGcm.encrypt(testDek, masterKey);

        EncryptionKey key = new EncryptionKey();
        key.setEncryptedKey(Base64.getEncoder().encodeToString(encryptedDek));
        when(keyRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(key));

        service = new EncryptionServiceImpl(keyRepository, TEST_MASTER_KEY_BASE64);
        service.init();
    }

    @Test
    void encryptReturnsEncPrefix() {
        assertTrue(service.encrypt("hello").startsWith("ENC:"));
    }

    @Test
    void encryptNullReturnsNull() {
        assertNull(service.encrypt(null));
    }

    @Test
    void decryptNullReturnsNull() {
        assertNull(service.decrypt(null));
    }

    @Test
    void decryptRoundTrip() {
        String plaintext = "sensitive post content";
        assertEquals(plaintext, service.decrypt(service.encrypt(plaintext)));
    }

    @Test
    void decryptUnencryptedPassthrough() {
        assertEquals("plaintext value", service.decrypt("plaintext value"));
    }

    @Test
    void isEncryptedTrueForEncPrefix() {
        assertTrue(service.isEncrypted("ENC:abc123"));
    }

    @Test
    void isEncryptedFalseForPlaintext() {
        assertFalse(service.isEncrypted("plain"));
    }

    @Test
    void isEncryptedFalseForNull() {
        assertFalse(service.isEncrypted(null));
    }

    @Test
    void encryptTwiceProducesDifferentCiphertexts() {
        assertNotEquals(service.encrypt("same"), service.encrypt("same"));
    }

    @Test
    void missingEncryptionKeyAutoGeneratesOnInit() {
        when(keyRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(keyRepository.save(any())).thenReturn(new EncryptionKey());
        EncryptionServiceImpl svc = new EncryptionServiceImpl(keyRepository, TEST_MASTER_KEY_BASE64);
        assertDoesNotThrow(svc::init);
        verify(keyRepository, times(1)).save(any());
        // After auto-gen, encrypt/decrypt should work
        String encrypted = svc.encrypt("test");
        assertEquals("test", svc.decrypt(encrypted));
    }

    @Test
    void dekLoadedOnceAtInit() {
        service.encrypt("call one");
        service.encrypt("call two");
        // Repository should only be called once (during init), not during encrypt
        verify(keyRepository, times(1)).findFirstByOrderByIdAsc();
    }
}
