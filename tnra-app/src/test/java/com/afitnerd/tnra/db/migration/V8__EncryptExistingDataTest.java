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

class V8__EncryptExistingDataTest {

    private static final String DEV_MASTER_KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private Context ctx;
    private Connection conn;
    private Statement stmt;
    private PreparedStatement ps;
    private ResultSet emptyRs;

    @BeforeEach
    void setUp() throws Exception {
        ctx = mock(Context.class);
        conn = mock(Connection.class);
        stmt = mock(Statement.class);
        ps = mock(PreparedStatement.class);
        emptyRs = mock(ResultSet.class);

        when(ctx.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(stmt.executeQuery(anyString())).thenReturn(emptyRs);
        when(emptyRs.next()).thenReturn(false);
    }

    @Test
    void migrateLoadsDekWhenPresent() throws Exception {
        ResultSet dekRs = dekResultSet();
        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        assertDoesNotThrow(() -> migration.migrate(ctx));

        // DEK loaded — no INSERT into encryption_keys
        verify(ps, never()).setString(anyInt(), anyString());
        verify(ps, never()).executeUpdate();
    }

    @Test
    void migrateGeneratesAndInsertsDekWhenAbsent() throws Exception {
        // encryption_keys returns no rows → generate new DEK
        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(emptyRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        assertDoesNotThrow(() -> migration.migrate(ctx));

        // INSERT into encryption_keys happened exactly once
        verify(ps, times(1)).setString(eq(1), anyString());
        verify(ps, times(1)).executeUpdate();
    }

    @Test
    void migrateEncryptsPlaintextPostColumn() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet postRs = plaintextRowResultSet("widwytk", "I did the thing", 7L);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("widwytk"))).thenReturn(postRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        migration.migrate(ctx);

        verify(ps, times(1)).executeUpdate();
        verify(ps, times(1)).setString(eq(1), argThat(v -> v.startsWith("ENC:")));
        verify(ps, times(1)).setLong(eq(2), eq(7L));
    }

    @Test
    void migrateSkipsAlreadyEncryptedValues() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet encRs = mock(ResultSet.class);
        when(encRs.next()).thenReturn(true, false);
        when(encRs.getString("kryptonite")).thenReturn("ENC:already");
        when(encRs.getLong("id")).thenReturn(3L);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("kryptonite"))).thenReturn(encRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        migration.migrate(ctx);

        verify(ps, never()).executeUpdate();
    }

    @Test
    void migrateEncryptsStatDefinitionColumns() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet nameRs = plaintextRowResultSet("name", "Exercise", 1L);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(argThat(q -> q.contains("stat_definition") && q.contains("name")))).thenReturn(nameRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        migration.migrate(ctx);

        verify(ps, atLeastOnce()).setString(eq(1), argThat(v -> v.startsWith("ENC:")));
    }

    @Test
    void migrateEncryptsPostStatValueColumn() throws Exception {
        ResultSet dekRs = dekResultSet();
        ResultSet psvRs = plaintextRowResultSet("stat_value", "42", 5L);

        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);
        when(stmt.executeQuery(contains("post_stat_value"))).thenReturn(psvRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        migration.migrate(ctx);

        verify(ps, atLeastOnce()).setString(eq(1), argThat(v -> v.startsWith("ENC:")));
        verify(ps, atLeastOnce()).setLong(eq(2), eq(5L));
    }

    @Test
    void migrateHandlesAllColumnsEmpty() throws Exception {
        ResultSet dekRs = dekResultSet();
        when(stmt.executeQuery(contains("encryption_keys"))).thenReturn(dekRs);

        V8__EncryptExistingData migration = new V8__EncryptExistingData(DEV_MASTER_KEY);
        assertDoesNotThrow(() -> migration.migrate(ctx));

        verify(ps, never()).executeUpdate();
    }

    private ResultSet dekResultSet() throws Exception {
        byte[] masterKey = Base64.getDecoder().decode(DEV_MASTER_KEY);
        byte[] rawDek = new byte[32];
        byte[] encryptedDek = AesGcm.encrypt(rawDek, masterKey);

        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString("encrypted_key"))
            .thenReturn(Base64.getEncoder().encodeToString(encryptedDek));
        return rs;
    }

    private ResultSet plaintextRowResultSet(String column, String value, long id) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(column)).thenReturn(value);
        when(rs.getLong("id")).thenReturn(id);
        return rs;
    }
}
