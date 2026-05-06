package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.GroupSettings;

public interface GroupSettingsService {
    GroupSettings getSettings();
    GroupSettings save(GroupSettings settings);
}
