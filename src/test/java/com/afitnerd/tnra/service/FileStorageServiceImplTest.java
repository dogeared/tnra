package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    void getFileUrlReturnsNullForBlankValuesAndPrefixesBaseUrl() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseUrl", "/files/");

        assertNull(service.getFileUrl(null));
        assertNull(service.getFileUrl(""));
        assertEquals("/files/image.webp", service.getFileUrl("image.webp"));
    }

    @Test
    void deleteFileRejectsUnsafePathTraversalInput() throws IOException {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());

        Path outsideFile = tempDir.getParent().resolve("outside.txt");
        Files.writeString(outsideFile, "keep");

        service.deleteFile("../outside.txt");

        assertTrue(Files.exists(outsideFile));
    }

    @Test
    void getFileUrlReturnsNullForUnsafeFileNames() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "baseUrl", "/files");

        assertNull(service.getFileUrl("../image.png"));
        assertNull(service.getFileUrl("nested/image.png"));
        assertNull(service.getFileUrl("image name.png"));
    }

    @Test
    void storeFileDropsUnsafeExtensionCharacters() throws IOException {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.resolve("uploads").toString());

        String fileName = service.storeFile(
            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
            "avatar.jp*g",
            "image/jpeg"
        );

        assertNotNull(fileName);
        assertEquals(-1, fileName.lastIndexOf('.'));
        assertTrue(Files.exists(Paths.get(tempDir.resolve("uploads").toString(), fileName)));
    }
}
