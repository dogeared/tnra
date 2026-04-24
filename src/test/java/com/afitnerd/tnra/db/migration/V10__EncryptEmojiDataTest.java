package com.afitnerd.tnra.db.migration;

import com.afitnerd.tnra.util.AesGcm;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class V10__EncryptEmojiDataTest {

    private static final String DEV_MASTER_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private Context ctx;
    private Connection conn;
    private Statement stmt;
    private PreparedStatement ps;

    @BeforeEach
    void setUp() throws Exception {
        ctx = mock(Context.class);
        conn = mock(Connection.class);
        stmt = mock(Statement.class);
        ps = mock(PreparedStatement.class);
        when(ctx.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
    }

    @Test
    void loadDekThrowsWhenEncryptionKeysEmpty() throws Exception {
        ResultSet emptyRs = mock(ResultSet.class);
        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(emptyRs);
        when(emptyRs.next()).thenReturn(false);

        V10__EncryptEmojiData migration = new V10__EncryptEmojiData();
        assertThrows(IllegalStateException.class, () -> migration.migrate(ctx));
    }

    @Test
    void migrateSkipsEmojiAlreadyPrefixedWithEnc() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet emojiRs = mock(ResultSet.class);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("stat_definition"))).thenReturn(emojiRs);
        when(emojiRs.next()).thenReturn(true, false);
        when(emojiRs.getString("emoji")).thenReturn("ENC:alreadyencrypted");

        V10__EncryptEmojiData migration = new V10__EncryptEmojiData();
        migration.migrate(ctx);

        verify(ps, never()).setString(anyInt(), anyString());
        verify(ps, never()).executeUpdate();
    }

    @Test
    void migrateEncryptsPlaintextEmoji() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet emojiRs = mock(ResultSet.class);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("stat_definition"))).thenReturn(emojiRs);
        when(emojiRs.next()).thenReturn(true, false);
        when(emojiRs.getString("emoji")).thenReturn("🏃");
        when(emojiRs.getLong("id")).thenReturn(42L);

        V10__EncryptEmojiData migration = new V10__EncryptEmojiData();
        migration.migrate(ctx);

        verify(ps, times(1)).executeUpdate();
        // Verify the SET param starts with ENC:
        verify(ps, times(1)).setString(eq(1), argThat(v -> v.startsWith("ENC:")));
        verify(ps, times(1)).setLong(eq(2), eq(42L));
    }

    @Test
    void migrateHandlesNoEmojiRows() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet emojiRs = mock(ResultSet.class);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("stat_definition"))).thenReturn(emojiRs);
        when(emojiRs.next()).thenReturn(false);

        V10__EncryptEmojiData migration = new V10__EncryptEmojiData();
        assertDoesNotThrow(() -> migration.migrate(ctx));
        verify(ps, never()).executeUpdate();
    }

    // Builds a ResultSet that returns one row with the dev DEK encrypted under the dev master key
    private ResultSet dekResultSet() throws Exception {
        byte[] masterKey = Base64.getDecoder().decode(DEV_MASTER_KEY);
        byte[] rawDek = new byte[32]; // zero-filled 256-bit key — deterministic for tests
        byte[] encryptedDek = AesGcm.encrypt(rawDek, masterKey);

        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString("encrypted_key"))
            .thenReturn(Base64.getEncoder().encodeToString(encryptedDek));
        return rs;
    }
}
