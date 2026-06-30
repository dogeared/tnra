package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.PaddleProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paddle Billing API client. Request/response shapes follow the documented Paddle Billing endpoints;
 * verify against a Paddle sandbox before production (you can't fully test until the seller account is
 * approved).
 *
 * <pre>
 *   createCheckout ─► POST /transactions
 *       body: items[{price_id, quantity}], customer.email, custom_data.{group_slug,beneficiary_email,
 *             payer_email}, collection_mode=automatic
 *       resp: data.checkout.url  (hosted checkout — requires a default payment link configured in the
 *             Paddle dashboard so Paddle can build the URL)
 *   getCustomerPortalUrl ─► POST /customers/{id}/portal-sessions
 *       resp: data.urls.general.overview
 *   cancelSubscription ─► POST /subscriptions/{id}/cancel  { effective_from: "immediately" }
 * </pre>
 */
@Service
public class PaddleClientImpl implements PaymentProviderClient {

    private final RestClient restClient;
    private final PaddleProperties props;

    public PaddleClientImpl(RestClient.Builder builder, PaddleProperties props) {
        this.props = props;
        this.restClient = builder.baseUrl(props.getApiBase()).build();
    }

    @Override
    public String createCheckout(String groupSlug, String beneficiaryEmail, String payerEmail,
                                 String variant, String redirectUrl) {
        String priceId = props.priceId(variant);

        // custom_data flows through to every subscription/transaction webhook, so the central service
        // can map a payment back to the right beneficiary account regardless of the Paddle customer.
        Map<String, Object> custom = new LinkedHashMap<>();
        custom.put("group_slug", groupSlug);
        custom.put("beneficiary_email", beneficiaryEmail);
        custom.put("payer_email", payerEmail);
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            custom.put("redirect_url", redirectUrl);
        }

        Map<String, Object> body = Map.of(
            "items", List.of(Map.of("price_id", priceId, "quantity", 1)),
            "customer", Map.of("email", payerEmail),
            "custom_data", custom,
            "collection_mode", "automatic"
        );

        JsonNode resp = restClient.post()
            .uri("/transactions")
            .header("Authorization", "Bearer " + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(JsonNode.class);

        return requireText(resp, "checkout url", "data", "checkout", "url");
    }

    @Override
    public String getCustomerPortalUrl(String customerId) {
        JsonNode resp = restClient.post()
            .uri("/customers/{id}/portal-sessions", customerId)
            .header("Authorization", "Bearer " + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of())
            .retrieve()
            .body(JsonNode.class);

        return requireText(resp, "customer portal url", "data", "urls", "general", "overview");
    }

    @Override
    public void cancelSubscription(String subscriptionId) {
        restClient.post()
            .uri("/subscriptions/{id}/cancel", subscriptionId)
            .header("Authorization", "Bearer " + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("effective_from", "immediately"))
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
            throw new IllegalStateException("Paddle response missing " + what);
        }
        return node.asText();
    }
}
