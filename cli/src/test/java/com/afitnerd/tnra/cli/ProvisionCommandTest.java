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

    private String[] provisionArgs(String groupName, String domain, Path registry, Path output) {
        return new String[]{
            "provision", groupName,
            "--domain", domain,
            "--registry", registry.toString(),
            "--output", output.toString(),
            "--admin-email", "admin@example.com",
            "--admin-first-name", "Test",
            "--admin-last-name", "Admin"
        };
    }

    @Test
    void happyPath() throws Exception {
        Path registry = tempDir.resolve("groups.json");
        Path output = tempDir.resolve("output");

        int code = run(provisionArgs("recovery-guys", "example.com", registry, output));

        assertEquals(0, code);

        Path groupDir = output.resolve("recovery-guys");
        assertTrue(Files.exists(groupDir.resolve("docker-compose.yml")));
        assertTrue(Files.exists(groupDir.resolve("recovery-guys-realm.json")));
        assertTrue(Files.exists(groupDir.resolve("recovery-guys.conf")));
        assertTrue(Files.exists(groupDir.resolve("init-db.sql")));
        assertTrue(Files.exists(groupDir.resolve("seed-admin.sql")));
        assertTrue(Files.exists(groupDir.resolve(".env")));
        assertTrue(Files.exists(groupDir.resolve("INSTRUCTIONS.md")));

        // Verify realm JSON contains group-specific values and admin user
        String realm = Files.readString(groupDir.resolve("recovery-guys-realm.json"));
        assertTrue(realm.contains("\"realm\": \"recovery-guys\""));
        assertTrue(realm.contains("\"clientId\": \"recovery-guys-app\""));
        assertTrue(realm.contains("recovery-guys.example.com"));
        assertTrue(realm.contains("admin@example.com"), "realm should include admin user");
        assertTrue(realm.contains("\"temporary\": true"), "admin password should be temporary");

        // Verify docker-compose embeds all per-group vars in environment block (no env_file)
        String compose = Files.readString(groupDir.resolve("docker-compose.yml"));
        assertFalse(compose.contains("env_file:"), "should not use env_file — all vars in environment block");
        assertTrue(compose.contains("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_ID: \"recovery-guys-app\""), "should embed client id as Spring property");
        assertTrue(compose.contains("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_KEYCLOAK_CLIENT_SECRET:"), "should embed client secret as Spring property");
        assertTrue(compose.contains("SPRING_DATASOURCE_URL: \"jdbc:mysql://mysql:3306/tnra_recovery_guys\""), "should use Docker-internal MySQL URL");
        assertTrue(compose.contains("TNRA_ENCRYPTION_MASTER_KEY: \"${TNRA_ENCRYPTION_MASTER_KEY}\""), "should use compose interpolation for encryption key");
        assertTrue(compose.contains("keycloak:8080/realms/recovery-guys"), "should use Docker-internal Keycloak for backchannel");
        assertTrue(compose.contains("tnra-production-shared"));

        // Verify .env is for local IDE dev only (localhost port, placeholder encryption key)
        String env = Files.readString(groupDir.resolve(".env"));
        assertTrue(env.contains("localhost:3307/tnra_recovery_guys"), ".env should use host-mapped MySQL port for IDE dev");
        assertTrue(env.contains("auth.example.com"), ".env should use Keycloak domain");
        assertTrue(env.contains("tnra_recovery_guys"), ".env should reference the group database");
        assertTrue(env.contains("TNRA_ENCRYPTION_MASTER_KEY="), ".env should include placeholder for encryption master key");

        // Verify SQL
        String sql = Files.readString(groupDir.resolve("init-db.sql"));
        assertTrue(sql.contains("tnra_recovery_guys"));

        // Verify seed-admin.sql creates the admin user in the database
        String seed = Files.readString(groupDir.resolve("seed-admin.sql"));
        assertTrue(seed.contains("admin@example.com"), "seed should insert admin email");
        assertTrue(seed.contains("tnra_recovery_guys"), "seed should target group database");

        // Verify uploads directory is created (placeholder copied only when
        // uploads/placeholder.png exists at project root, not during tests)
        assertTrue(Files.exists(groupDir.resolve("uploads/recovery-guys")),
            "should create uploads directory for the group");

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
            "--output", tempDir.resolve("out").toString(),
            "--admin-email", "a@b.com",
            "--admin-first-name", "A",
            "--admin-last-name", "B");
        assertEquals(1, code);
    }

    @Test
    void duplicateNameFails() {
        Path registry = tempDir.resolve("groups.json");
        String outDir = tempDir.resolve("out").toString();

        run(provisionArgs("my-group", "tnra.app", registry, Path.of(outDir)));
        int code = run(provisionArgs("my-group", "tnra.app", registry, Path.of(outDir)));

        assertEquals(1, code);
    }

    @Test
    void reservedNameFails() {
        int code = run("provision", "www",
            "--registry", tempDir.resolve("groups.json").toString(),
            "--output", tempDir.resolve("out").toString(),
            "--admin-email", "a@b.com",
            "--admin-first-name", "A",
            "--admin-last-name", "B");
        assertEquals(1, code);
    }
}
