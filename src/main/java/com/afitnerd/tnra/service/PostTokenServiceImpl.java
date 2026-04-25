package com.afitnerd.tnra.service;

import org.springframework.stereotype.Service;

@Service
public class PostTokenServiceImpl implements PostTokenService {

    private final EncryptionService encryptionService;

    public PostTokenServiceImpl(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String encode(Long postId) {
        String encrypted = encryptionService.encrypt(postId.toString());
        // encrypted = "ENC:" + standard_base64
        String base64 = encrypted.substring("ENC:".length());
        return base64.replace('+', '-').replace('/', '_').replaceAll("=+$", "");
    }

    @Override
    public Long decode(String token) {
        try {
            String base64 = token.replace('-', '+').replace('_', '/');
            int pad = (4 - base64.length() % 4) % 4;
            base64 += "=".repeat(pad);
            String decrypted = encryptionService.decrypt("ENC:" + base64);
            return Long.parseLong(decrypted);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid post token", e);
        }
    }
}
