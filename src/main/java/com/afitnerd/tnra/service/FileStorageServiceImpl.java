package com.afitnerd.tnra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private static final Pattern SAFE_FILE_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,255}$");
    private static final Pattern SAFE_EXTENSION_PATTERN = Pattern.compile("^[A-Za-z0-9]{1,10}$");

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
        if (StringUtils.hasText(fileExtension) && SAFE_EXTENSION_PATTERN.matcher(fileExtension).matches()) {
            uniqueFileName += "." + fileExtension.toLowerCase(Locale.ROOT);
        }

        // Store the file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    @Override
    public void deleteFile(String fileName) {
        String safeFileName = toSafeFileName(fileName);
        if (safeFileName == null) {
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(safeFileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return;
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw - file might already be deleted
            log.warn("Error deleting file {}: {}", safeFileName, e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        String safeFileName = toSafeFileName(fileName);
        if (safeFileName == null) {
            return null;
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/" + safeFileName;
    }

    private String toSafeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }

        String candidate = fileName.trim();
        String leafName = Paths.get(candidate).getFileName().toString();
        if (!candidate.equals(leafName)) {
            return null;
        }
        if (!SAFE_FILE_NAME_PATTERN.matcher(leafName).matches()) {
            return null;
        }
        return leafName;
    }
}
