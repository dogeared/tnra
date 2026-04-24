package com.afitnerd.tnra.db.migration;

import com.afitnerd.tnra.util.AesGcm;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

/**
 * Generates per-group DEK and encrypts all existing plaintext in sensitive columns.
 * Uses raw JDBC (not JPA) to avoid double-encryption through AttributeConverters.
 * Idempotent: skips values already prefixed with "ENC:".
 */
@Component
public class V8__EncryptExistingData extends BaseJavaMigration {

    private static final String ENC_PREFIX = "ENC:";

    private final String resolvedMasterKey;

    public V8__EncryptExistingData() {
        this.resolvedMasterKey = null;
    }

    V8__EncryptExistingData(String masterKeyBase64) {
        this.resolvedMasterKey = masterKeyBase64;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();

        String masterKeyBase64 = resolvedMasterKey != null ? resolvedMasterKey : System.getenv("TNRA_MASTER_KEY");
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalStateException(
                "TNRA_MASTER_KEY environment variable must be set before running migrations");
        }
        byte[] masterKey = Base64.getDecoder().decode(masterKeyBase64);

        byte[] dek = ensureDek(conn, masterKey);

        encryptPostColumns(conn, dek);
        encryptStatDefinitionColumns(conn, dek);
        encryptPostStatValueColumn(conn, dek);
    }

    private byte[] ensureDek(Connection conn, byte[] masterKey) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT encrypted_key FROM encryption_keys ORDER BY id ASC LIMIT 1")) {
            if (rs.next()) {
                byte[] encryptedDek = Base64.getDecoder().decode(rs.getString("encrypted_key"));
                return AesGcm.decrypt(encryptedDek, masterKey);
            }
        }
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        byte[] dek = keyGen.generateKey().getEncoded();
        byte[] encryptedDek = AesGcm.encrypt(dek, masterKey);
        String encryptedDekBase64 = Base64.getEncoder().encodeToString(encryptedDek);
        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO encryption_keys (encrypted_key, created_at) VALUES (?, NOW())")) {
            ps.setString(1, encryptedDekBase64);
            ps.executeUpdate();
        }
        return dek;
    }

    private void encryptPostColumns(Connection conn, byte[] dek) throws SQLException {
        String[] columns = {"widwytk", "kryptonite", "what_and_when",
            "personal_best", "personal_worst", "family_best", "family_worst", "work_best", "work_worst"};
        for (String col : columns) {
            encryptColumn(conn, "post", "id", col, dek);
        }
    }

    private void encryptStatDefinitionColumns(Connection conn, byte[] dek) throws SQLException {
        encryptColumn(conn, "stat_definition", "id", "name", dek);
        encryptColumn(conn, "stat_definition", "id", "label", dek);
    }

    private void encryptPostStatValueColumn(Connection conn, byte[] dek) throws SQLException {
        encryptColumn(conn, "post_stat_value", "id", "stat_value", dek);
    }

    private void encryptColumn(Connection conn, String table, String idCol, String col, byte[] dek)
        throws SQLException {
        String selectSql = "SELECT " + idCol + ", " + col + " FROM " + table + " WHERE " + col + " IS NOT NULL";
        String updateSql = "UPDATE " + table + " SET " + col + " = ? WHERE " + idCol + " = ?";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            while (rs.next()) {
                String value = rs.getString(col);
                if (value != null && !value.startsWith(ENC_PREFIX)) {
                    String encrypted = ENC_PREFIX + Base64.getEncoder().encodeToString(
                        AesGcm.encrypt(value.getBytes(StandardCharsets.UTF_8), dek));
                    ps.setString(1, encrypted);
                    ps.setLong(2, rs.getLong(idCol));
                    ps.executeUpdate();
                }
            }
        }
    }
}
