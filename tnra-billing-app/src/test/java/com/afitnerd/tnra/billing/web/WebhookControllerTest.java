package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebhookControllerTest {

    @Test
    void webhook_delegatesAndReturns200() {
        WebhookService service = mock(WebhookService.class);
        WebhookController controller = new WebhookController(service);

        ResponseEntity<Void> resp = controller.webhook("{\"body\":1}", "sig-123");

        assertEquals(200, resp.getStatusCode().value());
        verify(service).process("{\"body\":1}", "sig-123");
    }
}
