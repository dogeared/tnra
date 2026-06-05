package com.afitnerd.tnra.service;

public interface PostTokenService {
    String encode(Long postId);
    Long decode(String token); // throws IllegalArgumentException on invalid/tampered token
}
