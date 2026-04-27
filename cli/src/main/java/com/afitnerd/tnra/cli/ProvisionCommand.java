package com.afitnerd.tnra.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@Command(name = "provision", description = "Provision a new TNRA group")
public class ProvisionCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Group name (lowercase, letters/digits/hyphens)")
    private String groupName;

    @Option(names = "--domain", defaultValue = "tnra.app", description = "Base domain")
    private String domain;

    @Option(names = "--registry", defaultValue = "groups.json", description = "Path to groups.json")
    private String registryPath;

    @Option(names = "--output", defaultValue = "provision", description = "Output directory")
    private String outputDir;

    @Option(names = "--admin-email", required = true, description = "Admin user email address")
    private String adminEmail;

    @Option(names = "--admin-first-name", required = true, description = "Admin user first name")
    private String adminFirstName;

    @Option(names = "--admin-last-name", required = true, description = "Admin user last name")
    private String adminLastName;

    private final TemplateRenderer renderer = new TemplateRenderer();
    private final SecretGenerator secrets = new SecretGenerator();

    @Override
    public Integer call() {
        try {
            return execute();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    int execute() throws IOException {
        String error = GroupNameValidator.validate(groupName);
        if (error != null) {
            System.err.println("Invalid group name: " + error);
            return 1;
        }

        GroupRegistry registry = new GroupRegistry(Path.of(registryPath));

        if (registry.exists(groupName)) {
            System.err.println("Group '" + groupName + "' already exists in registry");
            return 1;
        }

        String dbName = "tnra_" + groupName.replace("-", "_");
        String dbUser = "tnra_" + groupName.replace("-", "_");
        String dbPassword = secrets.generate();
        String keycloakSecret = secrets.generate();
        String adminPassword = UUID.randomUUID().toString();
        String realmName = groupName;
        String date = LocalDate.now().toString();

        GroupRegistry.GroupEntry entry = registry.register(groupName, dbName, realmName, domain);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("GROUP_NAME", groupName);
        vars.put("DB_NAME", dbName);
        vars.put("DB_USER", dbUser);
        vars.put("DB_PASSWORD", dbPassword);
        vars.put("KEYCLOAK_CLIENT_SECRET", keycloakSecret);
        vars.put("REALM_NAME", realmName);
        vars.put("DOMAIN", domain);
        vars.put("DATE", date);
        vars.put("ADMIN_EMAIL", adminEmail);
        vars.put("ADMIN_FIRST_NAME", adminFirstName);
        vars.put("ADMIN_LAST_NAME", adminLastName);
        vars.put("ADMIN_PASSWORD", adminPassword);
        vars.put("PORT", String.valueOf(entry.port));

        Path outDir = Path.of(outputDir, groupName);
        Files.createDirectories(outDir);

        writeFile(outDir, "docker-compose.yml", renderer.render("docker-compose.yml.tmpl", vars));
        writeFile(outDir, groupName + "-realm.json", renderer.render("realm.json.tmpl", vars));
        writeFile(outDir, groupName + ".conf", renderer.render("nginx.conf.tmpl", vars));
        writeFile(outDir, "init-db.sql", renderer.render("init-db.sql.tmpl", vars));
        writeFile(outDir, "seed-admin.sql", renderer.render("seed-admin.sql.tmpl", vars));
        writeFile(outDir, ".env", renderer.render("env.tmpl", vars));
        writeFile(outDir, "INSTRUCTIONS.md", renderer.render("instructions.md.tmpl", vars));

        // Create uploads directory with placeholder image from project root
        Path uploadsDir = outDir.resolve("uploads").resolve(groupName);
        Files.createDirectories(uploadsDir);
        Path placeholderSource = Path.of("uploads", "placeholder.png");
        if (Files.exists(placeholderSource)) {
            Files.copy(placeholderSource, uploadsDir.resolve("placeholder.png"), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Provisioned group: " + groupName);
        System.out.println("  Domain:    " + groupName + "." + domain);
        System.out.println("  Database:  " + dbName);
        System.out.println("  Realm:     " + realmName);
        System.out.println("  Port:      " + entry.port);
        System.out.println("  Output:    " + outDir.toAbsolutePath());
        System.out.println();
        System.out.println("  Admin:     " + adminEmail);
        System.out.println("  Password:  " + adminPassword + " (temporary — must change on first login)");
        System.out.println();
        System.out.println("Next: read " + outDir.resolve("INSTRUCTIONS.md"));

        return 0;
    }

    private void writeFile(Path dir, String filename, String content) throws IOException {
        Files.writeString(dir.resolve(filename), content);
    }
}
