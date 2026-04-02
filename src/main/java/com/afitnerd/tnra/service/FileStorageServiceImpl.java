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
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);
    private static final Pattern SAFE_STORED_FILENAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[A-Za-z0-9]{1,10}");

    @Value("${app.file-storage.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file-storage.base-url:/uploads}")
    private String baseUrl;

    @Override
    public String storeFile(InputStream inputStream, String fileName, String contentType) throws IOException {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("fileName must not be blank");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename to prevent conflicts
        String fileExtension = sanitizeExtension(StringUtils.getFilenameExtension(fileName));
        String uniqueFileName = UUID.randomUUID().toString();
        if (StringUtils.hasText(fileExtension)) {
            uniqueFileName += "." + fileExtension;
        }

        // Store the file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    @Override
    public void deleteFile(String fileName) {
        if (!isSafeStoredFileName(fileName)) {
            if (StringUtils.hasText(fileName)) {
                log.warn("Ignoring unsafe filename for delete operation: {}", fileName);
            }
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                log.warn("Refusing to delete file outside upload directory: {}", fileName);
                return;
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw - file might already be deleted
            log.warn("Error deleting file: {}", fileName, e);
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        if (!isSafeStoredFileName(fileName)) {
            return null;
        }
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        return normalizedBaseUrl + "/" + fileName;
    }

    private static boolean isSafeStoredFileName(String fileName) {
        return StringUtils.hasText(fileName)
            && SAFE_STORED_FILENAME.matcher(fileName).matches()
            && !fileName.contains("..")
            && !fileName.contains("/")
            && !fileName.contains("\\");
    }

    private static String sanitizeExtension(String fileExtension) {
        if (!StringUtils.hasText(fileExtension)) {
            return null;
        }
        String normalized = fileExtension.trim().toLowerCase();
        return SAFE_EXTENSION.matcher(normalized).matches() ? normalized : null;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "/uploads";
        while (resolvedBaseUrl.endsWith("/")) {
            resolvedBaseUrl = resolvedBaseUrl.substring(0, resolvedBaseUrl.length() - 1);
        }
        return StringUtils.hasText(resolvedBaseUrl) ? resolvedBaseUrl : "/uploads";
    }
}
