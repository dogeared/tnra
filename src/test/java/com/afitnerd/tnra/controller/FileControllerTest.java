package com.afitnerd.tnra.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void serveFileReturnsResourceAndContentTypeForReadableFile() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("avatar.png"), "png");

        ResponseEntity<Resource> response = controller.serveFile("avatar.png");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertEquals("inline; filename=\"avatar.png\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertTrue(response.getBody().exists());
    }

    @Test
    void serveFileReturnsNotFoundWhenFileDoesNotExist() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        ResponseEntity<Resource> response = controller.serveFile("missing.gif");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void serveFileRejectsDirectoryTraversal() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        ResponseEntity<Resource> response = controller.serveFile("../secrets.txt");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void serveFileFallsBackToOctetStreamForUnknownExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("notes.bin"), "data");

        ResponseEntity<Resource> response = controller.serveFile("notes.bin");

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    }

    @Test
    void serveFileFallsBackToOctetStreamWhenNoExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("avatar"), "data");

        ResponseEntity<Resource> response = controller.serveFile("avatar");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    }
}
