package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;
import com.afitnerd.tnra.repository.GroupSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupSettingsServiceImpl implements GroupSettingsService {

    private final GroupSettingsRepository repository;

    public GroupSettingsServiceImpl(GroupSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public GroupSettings getSettings() {
        return repository.findFirstByOrderByIdAsc().orElseGet(GroupSettings::new);
    }

    @Override
    public GroupSettings save(GroupSettings settings) {
        return repository.save(settings);
    }
}
