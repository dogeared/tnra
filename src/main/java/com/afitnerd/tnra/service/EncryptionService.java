package com.afitnerd.tnra.service;

public interface EncryptionService {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);

    boolean isEncrypted(String value);
}
