package com.afitnerd.tnra.billing.repository;

import com.afitnerd.tnra.billing.model.BillingAccount;
import com.afitnerd.tnra.billing.model.BillingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BillingAccountRepositoryTest {

    @Autowired
    private BillingAccountRepository repository;

    private BillingAccount account(String group, String email, String payer) {
        BillingAccount a = new BillingAccount();
        a.setGroupSlug(group);
        a.setEmail(email);
        a.setPayerEmail(payer);
        a.setStatus(BillingStatus.ACTIVE);
        return a;
    }

    @Test
    void findByGroupSlugAndEmail_returnsBeneficiaryAccount() {
        repository.save(account("g1", "m@x.com", null));

        Optional<BillingAccount> found = repository.findByGroupSlugAndEmail("g1", "m@x.com");

        assertTrue(found.isPresent());
        assertEquals(BillingStatus.ACTIVE, found.get().getStatus());
    }

    @Test
    void findByGroupSlugAndEmail_emptyWhenAbsent() {
        assertTrue(repository.findByGroupSlugAndEmail("g1", "nobody@x.com").isEmpty());
    }

    @Test
    void findByLsSubscriptionId_routesWebhookToAccount() {
        BillingAccount a = account("g1", "m@x.com", null);
        a.setLsSubscriptionId("sub_123");
        repository.save(a);

        assertTrue(repository.findByLsSubscriptionId("sub_123").isPresent());
        assertTrue(repository.findByLsSubscriptionId("sub_nope").isEmpty());
    }

    @Test
    void findByLsCustomerId_findsAccount() {
        BillingAccount a = account("g1", "m@x.com", null);
        a.setLsCustomerId("cus_9");
        repository.save(a);

        assertTrue(repository.findByLsCustomerId("cus_9").isPresent());
    }

    @Test
    void findByGroupSlugAndPayerEmail_listsGiftsAPayerCovers() {
        repository.save(account("g1", "a@x.com", "admin@x.com"));
        repository.save(account("g1", "b@x.com", "admin@x.com"));
        repository.save(account("g1", "self@x.com", null));

        List<BillingAccount> covered = repository.findByGroupSlugAndPayerEmail("g1", "admin@x.com");

        assertEquals(2, covered.size());
    }

    @Test
    void uniqueConstraint_rejectsDuplicateGroupEmail() {
        repository.saveAndFlush(account("g1", "dup@x.com", null));

        assertThrows(DataIntegrityViolationException.class, () ->
            repository.saveAndFlush(account("g1", "dup@x.com", null)));
    }

    @Test
    void sameEmail_allowedInDifferentGroups() {
        repository.saveAndFlush(account("g1", "m@x.com", null));
        repository.saveAndFlush(account("g2", "m@x.com", null));

        assertFalse(repository.findByGroupSlugAndEmail("g1", "m@x.com").isEmpty());
        assertFalse(repository.findByGroupSlugAndEmail("g2", "m@x.com").isEmpty());
    }
}
