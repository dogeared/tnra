package com.afitnerd.tnra.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GroupRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromNonExistentFile() throws Exception {
        GroupRegistry registry = new GroupRegistry(tempDir.resolve("missing.json"));
        GroupRegistry.RegistryData data = registry.load();
        assertTrue(data.groups.isEmpty());
        assertEquals(8081, data.nextPort);
    }

    @Test
    void registerFirstGroup() throws Exception {
        GroupRegistry registry = new GroupRegistry(tempDir.resolve("groups.json"));
        GroupRegistry.GroupEntry entry = registry.register("test-group", "tnra_test_group", "test-group", "tnra.app");

        assertEquals("test-group", entry.name);
        assertEquals(8081, entry.port);
        assertEquals("active", entry.status);
    }

    @Test
    void registerSecondGroupIncrementsPort() throws Exception {
        Path path = tempDir.resolve("groups.json");
        GroupRegistry registry = new GroupRegistry(path);
        registry.register("group-one", "tnra_group_one", "group-one", "tnra.app");
        GroupRegistry.GroupEntry second = registry.register("group-two", "tnra_group_two", "group-two", "tnra.app");

        assertEquals(8082, second.port);
    }

    @Test
    void duplicateNameThrows() throws Exception {
        GroupRegistry registry = new GroupRegistry(tempDir.resolve("groups.json"));
        registry.register("my-group", "tnra_my_group", "my-group", "tnra.app");

        assertThrows(IllegalArgumentException.class,
            () -> registry.register("my-group", "tnra_my_group", "my-group", "tnra.app"));
    }

    @Test
    void existsReturnsTrueAfterRegister() throws Exception {
        GroupRegistry registry = new GroupRegistry(tempDir.resolve("groups.json"));
        assertFalse(registry.exists("test"));
        registry.register("test", "tnra_test", "test", "tnra.app");
        assertTrue(registry.exists("test"));
    }

    @Test
    void persistsAcrossInstances() throws Exception {
        Path path = tempDir.resolve("groups.json");
        new GroupRegistry(path).register("persisted", "tnra_persisted", "persisted", "tnra.app");

        GroupRegistry fresh = new GroupRegistry(path);
        assertTrue(fresh.exists("persisted"));
        assertEquals(8082, fresh.load().nextPort);
    }
}
