package com.afitnerd.tnra.cli;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingProvisioningClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(String contextPath, int status, String responseBody,
                               AtomicReference<String> capturedToken,
                               AtomicReference<String> capturedBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(contextPath, exchange -> {
            if (capturedToken != null) {
                capturedToken.set(exchange.getRequestHeaders().getFirst("X-Admin-Token"));
            }
            if (capturedBody != null) {
                capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void register_sendsTokenAndBody_returnsApiToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String url = startServer("/api/admin/groups", 200,
            "{\"groupSlug\":\"rome\",\"apiToken\":\"tok-xyz\"}", token, body);

        String result = new BillingProvisioningClient(url, "admin-secret").register("rome", 60, true);

        assertEquals("tok-xyz", result);
        assertEquals("admin-secret", token.get());
        assertTrue(body.get().contains("\"groupSlug\":\"rome\""));
        assertTrue(body.get().contains("\"trialDays\":60"));
        assertTrue(body.get().contains("\"exempt\":true"));
    }

    @Test
    void register_omitsTrialDaysWhenNull() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        String url = startServer("/api/admin/groups", 200, "{\"apiToken\":\"t\"}", null, body);

        new BillingProvisioningClient(url, "x").register("rome", null, false);

        assertTrue(!body.get().contains("trialDays"), "trialDays should be omitted when null");
    }

    @Test
    void register_nonSuccess_throws() throws Exception {
        String url = startServer("/api/admin/groups", 409, "{\"error\":\"exists\"}", null, null);

        IOException ex = assertThrows(IOException.class,
            () -> new BillingProvisioningClient(url, "x").register("rome", 60, false));
        assertTrue(ex.getMessage().contains("409"));
    }

    @Test
    void register_missingApiToken_throws() throws Exception {
        String url = startServer("/api/admin/groups", 200, "{\"groupSlug\":\"rome\"}", null, null);

        assertThrows(IOException.class,
            () -> new BillingProvisioningClient(url, "x").register("rome", 60, false));
    }
}
