package com.afitnerd.tnra.service;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {
    String storeFile(InputStream inputStream, String fileName, String contentType) throws IOException;
    void deleteFile(String fileName);
    String getFileUrl(String fileName);
} 