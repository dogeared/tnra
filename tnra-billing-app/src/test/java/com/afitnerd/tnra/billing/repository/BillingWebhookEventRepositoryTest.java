package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.BillingWebhookEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BillingWebhookEventRepositoryTest {

    @Autowired
    private BillingWebhookEventRepository repository;

    private BillingWebhookEvent event(String eventId) {
        BillingWebhookEvent e = new BillingWebhookEvent();
        e.setLsEventId(eventId);
        e.setEventName("subscription_created");
        e.setRawPayload("{\"meta\":{}}");
        return e;
    }

    @Test
    void existsByLsEventId_trueAfterPersist_falseOtherwise() {
        repository.save(event("evt_1"));

        assertTrue(repository.existsByLsEventId("evt_1"));
        assertFalse(repository.existsByLsEventId("evt_other"));
    }
}
