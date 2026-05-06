package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.GroupSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupSettingsRepository extends JpaRepository<GroupSettings, Long> {
    Optional<GroupSettings> findFirstByOrderByIdAsc();
}
