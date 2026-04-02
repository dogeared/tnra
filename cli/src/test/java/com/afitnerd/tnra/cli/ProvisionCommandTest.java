package com.afitnerd.tnra.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionCommandTest {

    @TempDir
    Path tempDir;

    private int run(String... args) {
        return new CommandLine(new TnraCli()).execute(args);
    }

    @Test
    void happyPath() throws Exception {
        Path registry = tempDir.resolve("groups.json");
        Path output = tempDir.resolve("output");

        int code = run("provision", "recovery-guys",
            "--domain", "example.com",
            "--registry", registry.toString(),
            "--output", output.toString());

        assertEquals(0, code);

        Path groupDir = output.resolve("recovery-guys");
        assertTrue(Files.exists(groupDir.resolve("docker-compose.yml")));
        assertTrue(Files.exists(groupDir.resolve("recovery-guys-realm.json")));
        assertTrue(Files.exists(groupDir.resolve("recovery-guys.conf")));
        assertTrue(Files.exists(groupDir.resolve("init-db.sql")));
        assertTrue(Files.exists(groupDir.resolve(".env")));
        assertTrue(Files.exists(groupDir.resolve("INSTRUCTIONS.md")));

        // Verify realm JSON contains group-specific values
        String realm = Files.readString(groupDir.resolve("recovery-guys-realm.json"));
        assertTrue(realm.contains("\"realm\": \"recovery-guys\""));
        assertTrue(realm.contains("\"clientId\": \"recovery-guys-app\""));
        assertTrue(realm.contains("recovery-guys.example.com"));
        assertFalse(realm.contains("admin@tnra.local")); // test users stripped

        // Verify docker-compose uses correct DB
        String compose = Files.readString(groupDir.resolve("docker-compose.yml"));
        assertTrue(compose.contains("tnra_recovery_guys"));
        assertTrue(compose.contains("tnra-shared"));

        // Verify SQL
        String sql = Files.readString(groupDir.resolve("init-db.sql"));
        assertTrue(sql.contains("tnra_recovery_guys"));

        // Verify registry updated
        assertTrue(Files.exists(registry));
        String registryJson = Files.readString(registry);
        assertTrue(registryJson.contains("recovery-guys"));
    }

    @Test
    void invalidNameFails() {
        Path registry = tempDir.resolve("groups.json");
        int code = run("provision", "BAD NAME!",
            "--registry", registry.toString(),
            "--output", tempDir.resolve("out").toString());
        assertEquals(1, code);
    }

    @Test
    void duplicateNameFails() {
        Path registry = tempDir.resolve("groups.json");
        String outDir = tempDir.resolve("out").toString();

        run("provision", "my-group", "--registry", registry.toString(), "--output", outDir);
        int code = run("provision", "my-group", "--registry", registry.toString(), "--output", outDir);

        assertEquals(1, code);
    }

    @Test
    void reservedNameFails() {
        int code = run("provision", "www",
            "--registry", tempDir.resolve("groups.json").toString(),
            "--output", tempDir.resolve("out").toString());
        assertEquals(1, code);
    }
}
