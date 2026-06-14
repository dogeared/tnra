package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.LemonSqueezyProperties;
import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.BillingWebhookEvent;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.BillingWebhookEventRepository;
import com.afitnerd.tnra.billing.util.HashUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Processes Lemon Squeezy webhooks. The flow is deliberately fail-loud on auth and never-lose on data:
 *
 * <pre>
 *   verify HMAC-SHA256(body, secret) == X-Signature   ── mismatch ─► 401 (nothing persisted)
 *   ls_event_id = sha256(body) (stable across retries)
 *   already seen?                                       ── yes ─► no-op
 *   persist raw event (always)
 *   route: subscription id ─► account, else custom_data {group_slug, beneficiary_email}
 *      matched   ─► update status + ls ids + payer, mark processed
 *      unmatched ─► leave matched_account_id null + error, for reconciliation (NOT dropped)
 * </pre>
 *
 * Lemon Squeezy subscription statuses → ours: active/on_trial → ACTIVE; past_due → ON_GRACE_PERIOD;
 * unpaid/cancelled/expired/paused → SUSPENDED. Event names override: subscription_payment_failed →
 * ON_GRACE_PERIOD; subscription_expired/cancelled → SUSPENDED.
 */
@Service
public class WebhookService {

    private final BillingWebhookEventRepository eventRepository;
    private final BillingAccountRepository accountRepository;
    private final LemonSqueezyProperties props;
    private final ObjectMapper objectMapper;

    public WebhookService(BillingWebhookEventRepository eventRepository,
                          BillingAccountRepository accountRepository,
                          LemonSqueezyProperties props,
                          ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountRepository = accountRepository;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void process(String body, String signature) {
        verifySignature(body, signature);

        String eventId = HashUtil.sha256Hex(body);
        if (eventRepository.existsByLsEventId(eventId)) {
            return; // idempotent: a redelivery of an event we already handled
        }

        JsonNode root;
        String eventName;
        try {
            root = objectMapper.readTree(body);
            eventName = text(root.path("meta").path("event_name"));
        } catch (Exception e) {
            persistUnprocessable(eventId, "unknown", body, "unparseable payload: " + e.getMessage());
            return;
        }

        BillingWebhookEvent event = new BillingWebhookEvent();
        event.setLsEventId(eventId);
        event.setEventName(eventName != null ? eventName : "unknown");
        event.setRawPayload(body);

        try {
            route(root, eventName, event);
        } catch (Exception e) {
            event.setProcessed(false);
            event.setError("processing error: " + e.getMessage());
        }
        eventRepository.save(event);
    }

    private void route(JsonNode root, String eventName, BillingWebhookEvent event) {
        JsonNode data = root.path("data");
        JsonNode attrs = data.path("attributes");
        JsonNode custom = root.path("meta").path("custom_data");

        String lsSubscriptionId = text(data.path("id"));
        String customerId = text(attrs.path("customer_id"));
        String lsStatus = text(attrs.path("status"));
        String groupSlug = text(custom.path("group_slug"));
        String beneficiary = text(custom.path("beneficiary_email"));
        String payer = text(custom.path("payer_email"));

        BillingAccount account = findAccount(lsSubscriptionId, groupSlug, beneficiary);
        if (account == null) {
            event.setProcessed(false);
            event.setError("unmatched: no account for subscription=" + lsSubscriptionId
                + " group=" + groupSlug + " beneficiary=" + beneficiary);
            return;
        }

        if (lsSubscriptionId != null) {
            account.setLsSubscriptionId(lsSubscriptionId);
        }
        if (customerId != null) {
            account.setLsCustomerId(customerId);
        }
        if (payer != null && beneficiary != null && !payer.equalsIgnoreCase(beneficiary)) {
            account.setPayerEmail(payer);
        }
        account.setStatus(mapStatus(eventName, lsStatus));
        accountRepository.save(account);

        event.setMatchedAccountId(account.getId());
        event.setProcessed(true);
    }

    private BillingAccount findAccount(String lsSubscriptionId, String groupSlug, String beneficiary) {
        if (lsSubscriptionId != null) {
            BillingAccount bySub = accountRepository.findByLsSubscriptionId(lsSubscriptionId).orElse(null);
            if (bySub != null) {
                return bySub;
            }
        }
        if (groupSlug != null && beneficiary != null) {
            return accountRepository.findByGroupSlugAndEmail(groupSlug, beneficiary).orElse(null);
        }
        return null;
    }

    BillingStatus mapStatus(String eventName, String lsStatus) {
        if ("subscription_payment_failed".equals(eventName)) {
            return BillingStatus.ON_GRACE_PERIOD;
        }
        if ("subscription_expired".equals(eventName) || "subscription_cancelled".equals(eventName)) {
            return BillingStatus.SUSPENDED;
        }
        if (lsStatus == null) {
            return BillingStatus.ON_GRACE_PERIOD; // unknown on a create/update: keep access, reconcile
        }
        return switch (lsStatus) {
            case "active", "on_trial" -> BillingStatus.ACTIVE;
            case "past_due" -> BillingStatus.ON_GRACE_PERIOD;
            case "unpaid", "cancelled", "expired", "paused" -> BillingStatus.SUSPENDED;
            default -> BillingStatus.ON_GRACE_PERIOD;
        };
    }

    private void verifySignature(String body, String signature) {
        if (props.getWebhookSecret() == null || props.getWebhookSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook secret not configured");
        }
        String expected = HashUtil.hmacSha256Hex(body, props.getWebhookSecret());
        if (!HashUtil.constantTimeEquals(expected, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
    }

    private void persistUnprocessable(String eventId, String eventName, String body, String error) {
        BillingWebhookEvent event = new BillingWebhookEvent();
        event.setLsEventId(eventId);
        event.setEventName(eventName);
        event.setRawPayload(body);
        event.setProcessed(false);
        event.setError(error);
        eventRepository.save(event);
    }

    /** JsonNode text accessor that returns null (not "null"/"") for missing/empty nodes. */
    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }
}
