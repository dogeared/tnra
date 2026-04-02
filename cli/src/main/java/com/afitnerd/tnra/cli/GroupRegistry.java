package com.afitnerd.tnra.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GroupRegistry {

    private static final int DEFAULT_START_PORT = 8081;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path registryPath;

    public GroupRegistry(Path registryPath) {
        this.registryPath = registryPath;
    }

    public RegistryData load() throws IOException {
        if (!Files.exists(registryPath)) {
            return new RegistryData(new ArrayList<>(), DEFAULT_START_PORT);
        }
        String json = Files.readString(registryPath);
        return GSON.fromJson(json, RegistryData.class);
    }

    public GroupEntry register(String name, String database, String realm, String domain) throws IOException {
        RegistryData data = load();

        if (data.groups.stream().anyMatch(g -> g.name.equals(name))) {
            throw new IllegalArgumentException("Group '" + name + "' already exists in registry");
        }

        int port = data.nextPort;
        if (port < 1024 || port > 65535) {
            throw new IllegalStateException("Port " + port + " is out of valid range");
        }

        GroupEntry entry = new GroupEntry(name, port, database, realm, domain,
            LocalDate.now().toString(), "active");
        data.groups.add(entry);
        data.nextPort = port + 1;

        Files.writeString(registryPath, GSON.toJson(data));
        return entry;
    }

    public boolean exists(String name) throws IOException {
        return load().groups.stream().anyMatch(g -> g.name.equals(name));
    }

    public static class RegistryData {
        public List<GroupEntry> groups;
        public int nextPort;

        public RegistryData(List<GroupEntry> groups, int nextPort) {
            this.groups = groups;
            this.nextPort = nextPort;
        }
    }

    public static class GroupEntry {
        public String name;
        public int port;
        public String database;
        public String realm;
        public String domain;
        public String provisioned;
        public String status;

        public GroupEntry(String name, int port, String database, String realm,
                          String domain, String provisioned, String status) {
            this.name = name;
            this.port = port;
            this.database = database;
            this.realm = realm;
            this.domain = domain;
            this.provisioned = provisioned;
            this.status = status;
        }
    }
}
