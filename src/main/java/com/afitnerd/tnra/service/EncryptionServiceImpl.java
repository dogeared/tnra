package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.EncryptionKey;
import com.afitnerd.tnra.repository.EncryptionKeyRepository;
import com.afitnerd.tnra.util.AesGcm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    static final String ENCRYPTED_PREFIX = "ENC:";

    private final EncryptionKeyRepository keyRepository;
    private final String masterKeyBase64;

    // Loaded (or generated) once at startup — never accessed from within a JPA session
    private byte[] dek;

    public EncryptionServiceImpl(
        EncryptionKeyRepository keyRepository,
        @Value("${tnra.encryption.master-key}") String masterKeyBase64
    ) {
        this.keyRepository = keyRepository;
        this.masterKeyBase64 = masterKeyBase64;
    }

    @PostConstruct
    void init() {
        byte[] masterKey = Base64.getDecoder().decode(masterKeyBase64);
        Optional<EncryptionKey> existing = keyRepository.findFirstByOrderByIdAsc();
        if (existing.isPresent()) {
            dek = AesGcm.decrypt(Base64.getDecoder().decode(existing.get().getEncryptedKey()), masterKey);
        } else {
            dek = generateAndPersistDek(masterKey);
        }
    }

    private byte[] generateAndPersistDek(byte[] masterKey) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            byte[] newDek = keyGen.generateKey().getEncoded();
            EncryptionKey key = new EncryptionKey();
            key.setEncryptedKey(Base64.getEncoder().encodeToString(AesGcm.encrypt(newDek, masterKey)));
            keyRepository.save(key);
            return newDek;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("AES key generation not available", e);
        }
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        byte[] ciphertext = AesGcm.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), dek);
        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(ciphertext);
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (!isEncrypted(ciphertext)) return ciphertext;
        byte[] decoded = Base64.getDecoder().decode(ciphertext.substring(ENCRYPTED_PREFIX.length()));
        return new String(AesGcm.decrypt(decoded, dek), StandardCharsets.UTF_8);
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }
}
