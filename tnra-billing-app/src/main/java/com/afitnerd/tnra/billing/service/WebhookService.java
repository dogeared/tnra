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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Processes Lemon Squeezy webhooks. Fail-loud on auth, never-lose on data:
 *
 * <pre>
 *   verify HMAC-SHA256(body, secret) == X-Signature   ── mismatch ─► 401 (nothing persisted)
 *   ls_event_id = sha256(body) (stable across retries)
 *   already seen?                                       ── yes ─► no-op
 *   persist raw event (always)
 *   route ─► matched: update status; unmatched: error + keep for reconciliation (never dropped)
 * </pre>
 *
 * Routing is subscription-id-first. The tricky case is GIFT SUPERSEDE: a beneficiary self-pays to
 * replace a gift, creating a NEW subscription. Its {@code subscription_created} routes by custom_data
 * to the account, which still points at the OLD gift subscription — we cancel the old one in Lemon
 * Squeezy (or the gifter keeps being charged), adopt the new one, and clear payer_email. Later events
 * for the OLD subscription id then no longer match the account's current sub and are ignored as stale,
 * so an old cancellation can't wrongly suspend a member who already took over.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final BillingWebhookEventRepository eventRepository;
    private final BillingAccountRepository accountRepository;
    private final LemonSqueezyClient lemonSqueezyClient;
    private final LemonSqueezyProperties props;
    private final ObjectMapper objectMapper;

    public WebhookService(BillingWebhookEventRepository eventRepository,
                          BillingAccountRepository accountRepository,
                          LemonSqueezyClient lemonSqueezyClient,
                          LemonSqueezyProperties props,
                          ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountRepository = accountRepository;
        this.lemonSqueezyClient = lemonSqueezyClient;
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

        String incomingSub = text(data.path("id"));
        String customerId = text(attrs.path("customer_id"));
        String lsStatus = text(attrs.path("status"));
        String groupSlug = text(custom.path("group_slug"));
        String beneficiary = text(custom.path("beneficiary_email"));
        String payer = text(custom.path("payer_email"));

        // 1) The account that already owns this subscription id — the normal update path.
        BillingAccount bySub = incomingSub == null ? null
            : accountRepository.findByLsSubscriptionId(incomingSub).orElse(null);
        if (bySub != null) {
            applyStatus(bySub, eventName, lsStatus);
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
            if ("subscription_created".equals(eventName)) {
                // Supersede: a new subscription replaces the one the account holds (e.g. self-pay
                // taking over a gift). Cancel the old sub in LS or the prior payer keeps being charged.
                cancelOldBestEffort(current, event);
                adopt(account, incomingSub, customerId, payer, beneficiary);
                applyStatus(account, eventName, lsStatus);
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
        applyStatus(account, eventName, lsStatus);
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
            lemonSqueezyClient.cancelSubscription(oldSubscriptionId);
        } catch (Exception e) {
            // Don't fail the new subscription over this — but flag it: the old sub may still bill the
            // prior payer until reconciled.
            log.warn("Failed to cancel superseded subscription {}: {}", oldSubscriptionId, e.getMessage());
            event.setError("WARNING: could not cancel superseded subscription " + oldSubscriptionId
                + " (" + e.getMessage() + ") — reconcile to stop billing the prior payer");
        }
    }

    private void applyStatus(BillingAccount account, String eventName, String lsStatus) {
        account.setStatus(mapStatus(eventName, lsStatus));
    }

    private void markMatched(BillingWebhookEvent event, BillingAccount account) {
        event.setMatchedAccountId(account.getId());
        event.setProcessed(true);
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
