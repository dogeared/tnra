package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.repository.GroupSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupSettingsServiceImplTest {

    private GroupSettingsRepository repository;
    private GroupSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(GroupSettingsRepository.class);
        service = new GroupSettingsServiceImpl(repository);
    }

    @Test
    void getSettings_returnsExistingRow() {
        GroupSettings existing = new GroupSettings();
        existing.setSlackWebhookUrl("https://hooks.slack.com/test");
        existing.setSlackEnabled(true);
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));

        GroupSettings result = service.getSettings();

        assertSame(existing, result);
        assertEquals("https://hooks.slack.com/test", result.getSlackWebhookUrl());
        assertTrue(result.isSlackEnabled());
    }

    @Test
    void getSettings_returnsNewInstanceWhenNoRowExists() {
        when(repository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        GroupSettings result = service.getSettings();

        assertNotNull(result);
        assertNull(result.getSlackWebhookUrl());
        assertFalse(result.isSlackEnabled());
    }

    @Test
    void newGroupSettings_hasNonNullTimestamps() {
        GroupSettings settings = new GroupSettings();
        assertNotNull(settings.getCreatedAt());
        assertNotNull(settings.getUpdatedAt());
    }

    @Test
    void save_delegatesToRepository() {
        GroupSettings settings = new GroupSettings();
        when(repository.save(settings)).thenReturn(settings);

        GroupSettings result = service.save(settings);

        assertSame(settings, result);
        verify(repository).save(settings);
    }
}
