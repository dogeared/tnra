package com.afitnerd.tnra.service;

import org.springframework.stereotype.Service;

@Service
public class PostTokenServiceImpl implements PostTokenService {

    private static final String ENC_PREFIX = "ENC:";

    private final EncryptionService encryptionService;

    public PostTokenServiceImpl(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String encode(Long postId) {
        String encrypted = encryptionService.encrypt(postId.toString());
        String base64 = encrypted.substring(ENC_PREFIX.length());
        return base64.replace('+', '-').replace('/', '_').replaceAll("=+$", "");
    }

    @Override
    public Long decode(String token) {
        try {
            String base64 = token.replace('-', '+').replace('_', '/');
            int pad = (4 - base64.length() % 4) % 4;
            base64 += "=".repeat(pad);
            String decrypted = encryptionService.decrypt(ENC_PREFIX + base64);
            return Long.parseLong(decrypted);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid post token", e);
        }
    }
}
