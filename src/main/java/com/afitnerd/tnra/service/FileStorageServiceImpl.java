package com.afitnerd.tnra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.file-storage.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file-storage.base-url:/uploads}")
    private String baseUrl;

    @Override
    public String storeFile(InputStream inputStream, String fileName, String contentType) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename to prevent conflicts
        String fileExtension = StringUtils.getFilenameExtension(fileName);
        String uniqueFileName = UUID.randomUUID().toString();
        if (fileExtension != null) {
            uniqueFileName += "." + fileExtension;
        }

        // Store the file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            try {
                Path filePath = Paths.get(uploadDir, fileName);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but don't throw - file might already be deleted
                System.err.println("Error deleting file: " + fileName + " - " + e.getMessage());
            }
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        return baseUrl + "/" + fileName;
    }
} 