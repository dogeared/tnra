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
    void serveFileReturnsImageJpegForJpgExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("photo.jpg"), "data");

        ResponseEntity<Resource> response = controller.serveFile("photo.jpg");

        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
    }

    @Test
    void serveFileReturnsImageJpegForJpegExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("photo.jpeg"), "data");

        ResponseEntity<Resource> response = controller.serveFile("photo.jpeg");

        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
    }

    @Test
    void serveFileReturnsImageGifForGifExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("anim.gif"), "data");

        ResponseEntity<Resource> response = controller.serveFile("anim.gif");

        assertEquals(MediaType.IMAGE_GIF, response.getHeaders().getContentType());
    }

    @Test
    void serveFileReturnsWebpContentTypeForWebpExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("img.webp"), "data");

        ResponseEntity<Resource> response = controller.serveFile("img.webp");

        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
    }

    @Test
    void serveFileReturnsSvgContentTypeForSvgExtension() throws IOException {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Files.writeString(tempDir.resolve("icon.svg"), "<svg/>");

        ResponseEntity<Resource> response = controller.serveFile("icon.svg");

        assertEquals(MediaType.parseMediaType("image/svg+xml"), response.getHeaders().getContentType());
    }
}
