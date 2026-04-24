package com.afitnerd.tnra.db.migration;

import com.afitnerd.tnra.util.AesGcm;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;

/**
 * Encrypts existing plaintext emoji values in stat_definition.
 * Uses raw JDBC to avoid double-encryption through AttributeConverters.
 * Idempotent: skips values already prefixed with "ENC:".
 */
@Component
public class V10__EncryptEmojiData extends BaseJavaMigration {

    private static final String ENC_PREFIX = "ENC:";
    private static final String DEFAULT_DEV_MASTER_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        String masterKeyBase64 = System.getenv("TNRA_MASTER_KEY");
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            masterKeyBase64 = DEFAULT_DEV_MASTER_KEY;
        }
        byte[] masterKey = Base64.getDecoder().decode(masterKeyBase64);
        byte[] dek = loadDek(conn, masterKey);

        String selectSql = "SELECT id, emoji FROM stat_definition WHERE emoji IS NOT NULL";
        String updateSql = "UPDATE stat_definition SET emoji = ? WHERE id = ?";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            while (rs.next()) {
                String value = rs.getString("emoji");
                if (value != null && !value.startsWith(ENC_PREFIX)) {
                    String encrypted = ENC_PREFIX + Base64.getEncoder().encodeToString(
                        AesGcm.encrypt(value.getBytes(StandardCharsets.UTF_8), dek));
                    ps.setString(1, encrypted);
                    ps.setLong(2, rs.getLong("id"));
                    ps.executeUpdate();
                }
            }
        }
    }

    private byte[] loadDek(Connection conn, byte[] masterKey) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT encrypted_key FROM encryption_keys ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                byte[] encryptedDek = Base64.getDecoder().decode(rs.getString("encrypted_key"));
                return AesGcm.decrypt(encryptedDek, masterKey);
            }
        }
        throw new IllegalStateException("No DEK found in encryption_keys — V8 migration must run first");
    }
}
