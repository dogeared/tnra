package com.afitnerd.tnra.billing.service;

import com.afitnerd.tnra.billing.config.PaddleProperties;
import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import com.afitnerd.tnra.billing.model.BillingWebhookEvent;
import com.afitnerd.tnra.billing.repository.BillingAccountRepository;
import com.afitnerd.tnra.billing.repository.BillingWebhookEventRepository;
import com.afitnerd.tnra.billing.util.HashUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Processes Paddle Billing webhooks. Fail-loud on auth, never-lose on data:
 *
 * <pre>
 *   verify Paddle-Signature (ts=…;h1=…): h1 == HMAC-SHA256(ts:body, secret)  ── mismatch ─► 401
 *   ls_event_id = sha256(body) (stable across redeliveries)
 *   already seen?                                       ── yes ─► no-op
 *   persist raw event (always)
 *   route ─► matched: update status; unmatched: error + keep for reconciliation (never dropped)
 * </pre>
 *
 * Routing is subscription-id-first. The tricky case is GIFT SUPERSEDE: a beneficiary self-pays to
 * replace a gift, creating a NEW subscription. Its {@code subscription.created} routes by custom_data
 * to the account, which still points at the OLD gift subscription — we cancel the old one in Paddle
 * (or the gifter keeps being charged), adopt the new one, and clear payer_email. Later events for the
 * OLD subscription id then no longer match the account's current sub and are ignored as stale, so an
 * old cancellation can't wrongly suspend a member who already took over.
 *
 * <p>Paddle sends both {@code subscription.*} events (where {@code data.id} is the subscription id) and
 * {@code transaction.*} events (where the subscription is {@code data.subscription_id}); both carry the
 * checkout's {@code data.custom_data}.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final BillingWebhookEventRepository eventRepository;
    private final BillingAccountRepository accountRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final PaddleProperties props;
    private final ObjectMapper objectMapper;

    public WebhookService(BillingWebhookEventRepository eventRepository,
                          BillingAccountRepository accountRepository,
                          PaymentProviderClient paymentProviderClient,
                          PaddleProperties props,
                          ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountRepository = accountRepository;
        this.paymentProviderClient = paymentProviderClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void process(String body, String signatureHeader) {
        verifySignature(body, signatureHeader);

        String eventId = HashUtil.sha256Hex(body);
        if (eventRepository.existsByLsEventId(eventId)) {
            return; // idempotent: a redelivery of an event we already handled
        }

        JsonNode root;
        String eventType;
        try {
            root = objectMapper.readTree(body);
            eventType = text(root.path("event_type"));
        } catch (Exception e) {
            persistUnprocessable(eventId, "unknown", body, "unparseable payload: " + e.getMessage());
            return;
        }

        BillingWebhookEvent event = new BillingWebhookEvent();
        event.setLsEventId(eventId);
        event.setEventName(eventType != null ? eventType : "unknown");
        event.setRawPayload(body);

        try {
            route(root, eventType, event);
        } catch (Exception e) {
            event.setProcessed(false);
            event.setError("processing error: " + e.getMessage());
        }
        eventRepository.save(event);
    }

    private void route(JsonNode root, String eventType, BillingWebhookEvent event) {
        JsonNode data = root.path("data");
        JsonNode custom = data.path("custom_data");

        // subscription.* events: data.id is the subscription id. transaction.* events: data.id is the
        // transaction id, so the subscription is data.subscription_id.
        boolean isSubscriptionEvent = eventType != null && eventType.startsWith("subscription.");
        String incomingSub = isSubscriptionEvent ? text(data.path("id")) : text(data.path("subscription_id"));
        String customerId = text(data.path("customer_id"));
        String status = text(data.path("status"));
        String groupSlug = text(custom.path("group_slug"));
        String beneficiary = text(custom.path("beneficiary_email"));
        String payer = text(custom.path("payer_email"));

        // 1) The account that already owns this subscription id — the normal update path.
        BillingAccount bySub = incomingSub == null ? null
            : accountRepository.findByLsSubscriptionId(incomingSub).orElse(null);
        if (bySub != null) {
            applyStatus(bySub, eventType, status);
            accountRepository.save(bySub);
            markMatched(event, bySub);
            return;
        }

        // 2) No account owns this sub id — route by custom data (group + beneficiary).
        BillingAccount account = (groupSlug != null && beneficiary != null)
            ? accountRepository.findByGroupSlugAndEmail(groupSlug, beneficiary).orElse(null)
            : null;
        if (account == null) {
            event.setProcessed(false);
            event.setError("unmatched: no account for subscription=" + incomingSub
                + " group=" + groupSlug + " beneficiary=" + beneficiary);
            return;
        }

        String current = account.getLsSubscriptionId();
        if (current != null && incomingSub != null && !current.equals(incomingSub)) {
            if ("subscription.created".equals(eventType)) {
                // Supersede: a new subscription replaces the one the account holds (e.g. self-pay
                // taking over a gift). Cancel the old sub in Paddle or the prior payer keeps being charged.
                cancelOldBestEffort(current, event);
                adopt(account, incomingSub, customerId, payer, beneficiary);
                applyStatus(account, eventType, status);
                accountRepository.save(account);
                markMatched(event, account);
            } else {
                // Stale event about a subscription this account no longer holds — record, ignore status.
                event.setMatchedAccountId(account.getId());
                event.setProcessed(true);
                event.setError("ignored stale event for superseded subscription " + incomingSub);
            }
            return;
        }

        // 3) First subscription for this beneficiary (account had no sub id yet).
        adopt(account, incomingSub, customerId, payer, beneficiary);
        applyStatus(account, eventType, status);
        accountRepository.save(account);
        markMatched(event, account);
    }

    /** Set sub/customer ids and payer (gift vs self-pay) from the event's custom data. */
    private void adopt(BillingAccount account, String incomingSub, String customerId,
                       String payer, String beneficiary) {
        if (incomingSub != null) {
            account.setLsSubscriptionId(incomingSub);
        }
        if (customerId != null) {
            account.setLsCustomerId(customerId);
        }
        if (payer != null && beneficiary != null) {
            // self-pay clears any prior gift payer; a gift records the gifter
            account.setPayerEmail(payer.equalsIgnoreCase(beneficiary) ? null : payer);
        }
    }

    private void cancelOldBestEffort(String oldSubscriptionId, BillingWebhookEvent event) {
        try {
            paymentProviderClient.cancelSubscription(oldSubscriptionId);
        } catch (Exception e) {
            // Don't fail the new subscription over this — but flag it: the old sub may still bill the
            // prior payer until reconciled.
            log.warn("Failed to cancel superseded subscription {}: {}", oldSubscriptionId, e.getMessage());
            event.setError("WARNING: could not cancel superseded subscription " + oldSubscriptionId
                + " (" + e.getMessage() + ") — reconcile to stop billing the prior payer");
        }
    }

    private void applyStatus(BillingAccount account, String eventType, String status) {
        account.setStatus(mapStatus(eventType, status));
    }

    private void markMatched(BillingWebhookEvent event, BillingAccount account) {
        event.setMatchedAccountId(account.getId());
        event.setProcessed(true);
    }

    /** Map a Paddle event type + subscription status to our entitlement status. */
    BillingStatus mapStatus(String eventType, String status) {
        if ("transaction.payment_failed".equals(eventType) || "subscription.past_due".equals(eventType)) {
            return BillingStatus.ON_GRACE_PERIOD;
        }
        if ("subscription.canceled".equals(eventType) || "subscription.paused".equals(eventType)) {
            return BillingStatus.SUSPENDED;
        }
        if ("transaction.completed".equals(eventType) || "subscription.activated".equals(eventType)
                || "subscription.resumed".equals(eventType)) {
            return BillingStatus.ACTIVE;
        }
        if (status == null) {
            return BillingStatus.ON_GRACE_PERIOD; // unknown on a create/update: keep access, reconcile
        }
        return switch (status) {
            case "active", "trialing" -> BillingStatus.ACTIVE;
            case "past_due" -> BillingStatus.ON_GRACE_PERIOD;
            case "paused", "canceled" -> BillingStatus.SUSPENDED;
            default -> BillingStatus.ON_GRACE_PERIOD;
        };
    }

    /**
     * Verify Paddle's {@code Paddle-Signature: ts=<unix>;h1=<hex>} header: h1 must equal
     * HMAC-SHA256(secret, "{ts}:{rawBody}"). The body must be the exact raw bytes Paddle signed.
     * (Replay is additionally bounded by idempotency on the event hash; we don't hard-reject on the
     * timestamp to avoid false failures from clock skew.)
     */
    private void verifySignature(String body, String signatureHeader) {
        if (props.getWebhookSecret() == null || props.getWebhookSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook secret not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Paddle-Signature");
        }
        String ts = null;
        String h1 = null;
        for (String part : signatureHeader.split(";")) {
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            if ("ts".equals(key)) {
                ts = value;
            } else if ("h1".equals(key)) {
                h1 = value;
            }
        }
        if (ts == null || h1 == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed Paddle-Signature");
        }
        String expected = HashUtil.hmacSha256Hex(ts + ":" + body, props.getWebhookSecret());
        if (!HashUtil.constantTimeEquals(expected, h1)) {
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
