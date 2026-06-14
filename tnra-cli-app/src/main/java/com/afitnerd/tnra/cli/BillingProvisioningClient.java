package com.afitnerd.tnra.cli;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Registers a newly provisioned group with the central billing app and returns its per-group API
 * token. Used only when {@code --billing-api-url} is supplied; offline / self-host provisioning
 * skips this entirely.
 */
public class BillingProvisioningClient {

    private final String apiUrl;
    private final String adminToken;
    private final HttpClient httpClient;

    public BillingProvisioningClient(String apiUrl, String adminToken) {
        this(apiUrl, adminToken, HttpClient.newHttpClient());
    }

    BillingProvisioningClient(String apiUrl, String adminToken, HttpClient httpClient) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.adminToken = adminToken == null ? "" : adminToken;
        this.httpClient = httpClient;
    }

    /** @return the plaintext per-group API token to write into the group's environment. */
    public String register(String groupSlug, Integer trialDays, boolean exempt)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("groupSlug", groupSlug);
        if (trialDays != null) {
            body.addProperty("trialDays", trialDays);
        }
        body.addProperty("exempt", exempt);

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/api/admin/groups"))
            .header("X-Admin-Token", adminToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Billing registration failed: HTTP " + response.statusCode()
                + " " + response.body());
        }
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!json.has("apiToken") || json.get("apiToken").isJsonNull()) {
            throw new IOException("Billing registration response missing apiToken");
        }
        return json.get("apiToken").getAsString();
    }
}
