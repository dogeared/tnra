package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void storeFileCreatesDirectoryAndPreservesExtension() throws IOException {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.resolve("uploads").toString());

        String fileName = service.storeFile(
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
            "avatar.png",
            "image/png"
        );

        assertNotNull(fileName);
        assertTrue(fileName.endsWith(".png"));
        Path storedFile = tempDir.resolve("uploads").resolve(fileName);
        assertTrue(Files.exists(storedFile));
        assertEquals("hello", Files.readString(storedFile));
    }

    @Test
    void deleteFileRemovesExistingFileAndIgnoresInvalidInput() throws IOException {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        Path storedFile = tempDir.resolve("sample.txt");
        Files.writeString(storedFile, "content");

        service.deleteFile("sample.txt");
        service.deleteFile("");
        service.deleteFile(null);

        assertTrue(Files.notExists(storedFile));
    }

    @Test
    void deleteFileRejectsPathTraversalOutsideUploadDirectory() throws IOException {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        Path uploadPath = tempDir.resolve("uploads");
        Path outsideFile = tempDir.resolve("outside.txt");
        ReflectionTestUtils.setField(service, "uploadDir", uploadPath.toString());

        Files.createDirectories(uploadPath);
        Files.writeString(outsideFile, "should-stay");

        service.deleteFile("../outside.txt");

        assertTrue(Files.exists(outsideFile));
    }

    @Test
    void getFileUrlReturnsNullForBlankValuesAndPrefixesBaseUrl() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseUrl", "/files");

        assertNull(service.getFileUrl(null));
        assertNull(service.getFileUrl(""));
        assertEquals("/files/image.webp", service.getFileUrl("image.webp"));
    }
}
