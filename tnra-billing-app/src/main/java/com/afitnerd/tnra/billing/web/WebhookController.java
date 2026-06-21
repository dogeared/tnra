package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lemon Squeezy webhook sink (anonymous at the Spring layer; authenticity is the HMAC signature).
 * Reads the RAW body so the signature is verified over exactly what Lemon Squeezy signed. Always
 * returns 200 once the event is persisted (so Lemon Squeezy stops retrying); an invalid signature
 * is the only 4xx.
 */
@RestController
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/api/billing/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String body,
                                        @RequestHeader(value = "X-Signature", required = false) String signature) {
        webhookService.process(body, signature);
        return ResponseEntity.ok().build();
    }
}
