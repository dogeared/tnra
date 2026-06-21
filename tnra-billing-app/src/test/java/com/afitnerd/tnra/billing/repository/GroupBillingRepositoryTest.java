package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.GroupBilling;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GroupBillingRepositoryTest {

    @Autowired
    private GroupBillingRepository repository;

    @Test
    void findByGroupSlug_returnsRowWithDefaults() {
        repository.save(new GroupBilling("rome", "hashed-token"));

        GroupBilling found = repository.findByGroupSlug("rome").orElseThrow();

        assertEquals("hashed-token", found.getApiTokenHash());
        assertFalse_exemptDefaultsFalse(found);
        assertTrue(found.getCreatedAt() != null);
    }

    private void assertFalse_exemptDefaultsFalse(GroupBilling g) {
        assertEquals(Boolean.FALSE, g.getExempt());
    }

    @Test
    void findByGroupSlug_emptyWhenAbsent() {
        assertTrue(repository.findByGroupSlug("missing").isEmpty());
    }
}
