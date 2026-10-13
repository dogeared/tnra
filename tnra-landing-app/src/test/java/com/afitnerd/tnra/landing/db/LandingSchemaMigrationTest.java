package com.afitnerd.tnra.landing.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard: the landing app runs Flyway + {@code ddl-auto=validate} against MySQL, so without
 * this migration on the classpath a fresh database fails startup with "missing table [request_access]".
 * The repo's {@code *.sql} ignore rule once swallowed per-module migrations; pin the file and its columns.
 */
class LandingSchemaMigrationTest {

    static final String MIGRATION = "/db/migration/V1__request_access.sql";

    @Test
    void v1_createsRequestAccessWithTheEntityColumns() throws IOException {
        String sql;
        try (InputStream in = LandingSchemaMigrationTest.class.getResourceAsStream(MIGRATION)) {
            assertNotNull(in, MIGRATION + " must be on the classpath (is it gitignored?)");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(Pattern.compile("CREATE TABLE\\s+(IF NOT EXISTS\\s+)?request_access\\s*\\(", Pattern.CASE_INSENSITIVE)
            .matcher(sql).find(), "V1 must create request_access");
        for (String column : new String[] {"group_name", "contact_name", "email", "estimated_size",
                "description", "submitted_at", "ip_address"}) {
            assertTrue(Pattern.compile("^\\s*" + column + "\\s", Pattern.MULTILINE).matcher(sql).find(),
                "V1 must declare column " + column);
        }
    }
}
