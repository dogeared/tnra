package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.LemonSqueezyProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Lemon Squeezy v1 API client (JSON:API). Request/response shapes follow the documented v1
 * checkout + subscription endpoints; verify against a live store before production.
 *
 * <pre>
 *   createCheckout ─► POST /v1/checkouts
 *       body: data.attributes.checkout_data.{email, custom:{group_slug,beneficiary_email,payer_email}}
 *             data.relationships.{store, variant}
 *       resp: data.attributes.url  (hosted checkout)
 *   getCustomerPortalUrl ─► GET /v1/subscriptions/{id}
 *       resp: data.attributes.urls.customer_portal
 * </pre>
 */
@Service
public class LemonSqueezyClientImpl implements LemonSqueezyClient {

    static final String API_BASE = "https://api.lemonsqueezy.com";
    private static final MediaType JSON_API = MediaType.parseMediaType("application/vnd.api+json");

    private final RestClient restClient;
    private final LemonSqueezyProperties props;

    public LemonSqueezyClientImpl(RestClient.Builder builder, LemonSqueezyProperties props) {
        this.restClient = builder.baseUrl(API_BASE).build();
        this.props = props;
    }

    @Override
    public String createCheckout(String groupSlug, String beneficiaryEmail, String payerEmail,
                                 String variant) {
        String variantId = props.variantId(variant);

        Map<String, Object> body = Map.of(
            "data", Map.of(
                "type", "checkouts",
                "attributes", Map.of(
                    "checkout_data", Map.of(
                        "email", payerEmail,
                        "custom", Map.of(
                            "group_slug", groupSlug,
                            "beneficiary_email", beneficiaryEmail,
                            "payer_email", payerEmail
                        )
                    )
                ),
                "relationships", Map.of(
                    "store", Map.of("data", Map.of("type", "stores", "id", props.getStoreId())),
                    "variant", Map.of("data", Map.of("type", "variants", "id", variantId))
                )
            )
        );

        JsonNode resp = restClient.post()
            .uri("/v1/checkouts")
            .header("Authorization", "Bearer " + props.getApiKey())
            .contentType(JSON_API)
            .accept(JSON_API)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        return requireText(resp, "checkout url", "data", "attributes", "url");
    }

    @Override
    public String getCustomerPortalUrl(String lsSubscriptionId) {
        JsonNode resp = restClient.get()
            .uri("/v1/subscriptions/{id}", lsSubscriptionId)
            .header("Authorization", "Bearer " + props.getApiKey())
            .accept(JSON_API)
            .retrieve()
            .body(JsonNode.class);

        return requireText(resp, "customer portal url", "data", "attributes", "urls", "customer_portal");
    }

    @Override
    public void cancelSubscription(String lsSubscriptionId) {
        restClient.delete()
            .uri("/v1/subscriptions/{id}", lsSubscriptionId)
            .header("Authorization", "Bearer " + props.getApiKey())
            .accept(JSON_API)
            .retrieve()
            .toBodilessEntity();
    }

    private String requireText(JsonNode root, String what, String... path) {
        JsonNode node = root;
        for (String key : path) {
            if (node == null) {
                break;
            }
            node = node.get(key);
        }
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalStateException("Lemon Squeezy response missing " + what);
        }
        return node.asText();
    }
}
