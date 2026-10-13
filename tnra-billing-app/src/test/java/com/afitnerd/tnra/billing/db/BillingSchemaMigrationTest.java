package com.afitnerd.tnra.billing.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard: the service runs Flyway + {@code ddl-auto=validate} against MySQL, so if the V1
 * migration is missing from the classpath the first MySQL boot fails with "missing table
 * [billing_account]". The unit tests run on H2 with Flyway off and never noticed — and the repo's
 * {@code *.sql} ignore rule once swallowed this exact file. Pin its presence and the tables it creates.
 */
class BillingSchemaMigrationTest {

    static final String MIGRATION = "/db/migration/V1__billing_schema.sql";

    private static String migration() throws IOException {
        try (InputStream in = BillingSchemaMigrationTest.class.getResourceAsStream(MIGRATION)) {
            assertNotNull(in, MIGRATION + " must be on the classpath (is it gitignored?)");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void v1_createsEveryEntityTable() throws IOException {
        String sql = migration();
        for (String table : new String[] {"group_billing", "billing_account", "billing_webhook_event"}) {
            assertTrue(Pattern.compile("CREATE TABLE\\s+" + table + "\\s*\\(", Pattern.CASE_INSENSITIVE)
                .matcher(sql).find(), "V1 must create " + table);
        }
    }

    @Test
    void v1_declaresTheColumnsTheEntitiesMap() throws IOException {
        String sql = migration();
        // Spot-check the columns the webhook/entitlement paths depend on, by their @Column names.
        for (String column : new String[] {"api_token_hash", "comp_until", "payer_email", "ls_subscription_id",
                "ls_customer_id", "ls_variant", "ls_event_id", "raw_payload", "matched_account_id", "received_at"}) {
            Matcher m = Pattern.compile("^\\s*" + column + "\\s", Pattern.MULTILINE).matcher(sql);
            assertTrue(m.find(), "V1 must declare column " + column);
        }
        assertTrue(sql.contains("uq_group_email"), "one account per (group, email)");
        assertTrue(sql.contains("uq_ls_event_id"), "webhook idempotency key");
    }
}
