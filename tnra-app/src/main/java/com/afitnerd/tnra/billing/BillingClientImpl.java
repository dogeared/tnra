package com.afitnerd.tnra.billing;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Talks to the central billing service over its per-group-token-authenticated API.
 *
 * <pre>
 *   isEntitled ─► GET /api/v1/entitlement   cached {@value #CACHE_TTL_MILLIS}ms; FAILS OPEN on error
 *   createCheckout ─► POST /api/v1/checkout  (errors surface so the member can retry)
 *   portalUrl ─► GET /api/v1/portal
 *   covering ─► GET /api/v1/covering
 * </pre>
 */
@Service
@ConditionalOnProperty(prefix = "tnra.billing", name = "enabled", havingValue = "true")
public class BillingClientImpl implements BillingClient {

    private static final Logger log = LoggerFactory.getLogger(BillingClientImpl.class);
    static final long CACHE_TTL_MILLIS = 60_000;

    private final RestClient restClient;
    private final Map<String, CacheEntry> entitlementCache = new ConcurrentHashMap<>();

    private record CacheEntry(boolean entitled, long expiresAt) {}

    public BillingClientImpl(RestClient.Builder builder,
                             @Value("${tnra.billing.api-url}") String apiUrl,
                             @Value("${tnra.billing.api-token}") String apiToken) {
        this.restClient = builder
            .baseUrl(apiUrl)
            .defaultHeader("Authorization", "Bearer " + apiToken)
            .build();
    }

    @Override
    public boolean isEntitled(String email) {
        CacheEntry cached = entitlementCache.get(email);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached.entitled();
        }
        return fetchAndCache(email);
    }

    @Override
    public boolean isEntitledFresh(String email) {
        return fetchAndCache(email);
    }

    @Override
    public Entitlement entitlement(String email) {
        try {
            JsonNode resp = restClient.get()
                .uri(b -> b.path("/api/v1/entitlement").queryParam("email", email).build())
                .retrieve()
                .body(JsonNode.class);
            boolean entitled = resp != null && resp.path("entitled").asBoolean(false);
            String status = resp == null ? "" : resp.path("status").asText("");
            String payerEmail = resp == null ? null : emptyToNull(resp.path("payerEmail").asText(""));
            String reason = resp == null ? null : emptyToNull(resp.path("reason").asText(""));
            return new Entitlement(entitled, status, payerEmail, reason);
        } catch (Exception e) {
            // Can't determine — report "not settled" so the gift can proceed; the billing service is the
            // authoritative guard (returns 409 on the actual checkout if the member is in fact covered).
            log.warn("Billing entitlement detail unreachable for {}: {}", email, e.getMessage());
            return new Entitlement(false, "", null);
        }
    }

    private boolean fetchAndCache(String email) {
        try {
            JsonNode resp = restClient.get()
                .uri(b -> b.path("/api/v1/entitlement").queryParam("email", email).build())
                .retrieve()
                .body(JsonNode.class);
            boolean entitled = resp != null && resp.path("entitled").asBoolean(false);
            entitlementCache.put(email, new CacheEntry(entitled, System.currentTimeMillis() + CACHE_TTL_MILLIS));
            return entitled;
        } catch (Exception e) {
            // Fail OPEN: never lock out a paying member because the billing service is unreachable.
            log.warn("Billing service unreachable for entitlement check of {}; failing open: {}",
                email, e.getMessage());
            return true;
        }
    }

    @Override
    public String createCheckout(String beneficiaryEmail, String variant, String payerEmail,
                                 String redirectUrl) {
        JsonNode resp = restClient.post()
            .uri("/api/v1/checkout")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "beneficiaryEmail", beneficiaryEmail,
                "variant", variant,
                "payerEmail", payerEmail == null ? "" : payerEmail,
                "redirectUrl", redirectUrl == null ? "" : redirectUrl))
            .retrieve()
            .body(JsonNode.class);
        return text(resp, "url");
    }

    @Override
    public String portalUrl(String email) {
        JsonNode resp = restClient.get()
            .uri(b -> b.path("/api/v1/portal").queryParam("email", email).build())
            .retrieve()
            .body(JsonNode.class);
        return text(resp, "url");
    }

    @Override
    public String paymentMethodUpdateUrl(String email) {
        JsonNode resp = restClient.get()
            .uri(b -> b.path("/api/v1/payment-method").queryParam("email", email).build())
            .retrieve()
            .body(JsonNode.class);
        return text(resp, "url");
    }

    @Override
    public Optional<Subscription> subscription(String email) {
        try {
            JsonNode resp = restClient.get()
                .uri(b -> b.path("/api/v1/subscription").queryParam("email", email).build())
                .retrieve()
                .body(JsonNode.class);
            if (resp == null) {
                return Optional.empty();
            }
            return Optional.of(new Subscription(
                emptyToNull(resp.path("providerStatus").asText("")),
                emptyToNull(resp.path("variant").asText("")),
                date(resp, "paidThrough"), date(resp, "nextBilledAt"),
                date(resp, "cancelAt"), date(resp, "trialEndsAt")));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Subscription details unreachable for {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    private static LocalDate date(JsonNode node, String field) {
        String s = emptyToNull(node.path(field).asText(""));
        return s == null ? null : java.time.Instant.parse(s).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    @Override
    public List<CoveredMember> covering(String payerEmail) {
        JsonNode resp = restClient.get()
            .uri(b -> b.path("/api/v1/covering").queryParam("payerEmail", payerEmail).build())
            .retrieve()
            .body(JsonNode.class);
        if (resp == null || !resp.isArray()) {
            return List.of();
        }
        List<CoveredMember> members = new ArrayList<>();
        for (JsonNode n : resp) {
            members.add(new CoveredMember(n.path("email").asText(), n.path("status").asText(), date(n, "cancelAt")));
        }
        return members;
    }

    @Override
    public LocalDate cancelOwn(String email) {
        JsonNode resp = restClient.post()
            .uri("/api/v1/cancel")
            .body(Map.of("email", email))
            .retrieve()
            .body(JsonNode.class);
        return resp == null ? null : date(resp, "endsAt");
    }

    @Override
    public void resumeOwn(String email) {
        restClient.post()
            .uri("/api/v1/resume")
            .body(Map.of("email", email))
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public LocalDate stopCovering(String payerEmail, String beneficiaryEmail) {
        JsonNode resp = restClient.post()
            .uri("/api/v1/covering/cancel")
            .body(Map.of("payerEmail", payerEmail, "beneficiaryEmail", beneficiaryEmail))
            .retrieve()
            .body(JsonNode.class);
        return resp == null ? null : date(resp, "endsAt");
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            throw new IllegalStateException("Billing service response missing " + field);
        }
        return node.get(field).asText();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
