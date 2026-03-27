package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.StatDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatDefinitionRepository extends JpaRepository<StatDefinition, Long> {

    List<StatDefinition> findByArchivedFalseOrderByDisplayOrderAsc();

    List<StatDefinition> findAllByOrderByDisplayOrderAsc();

    Optional<StatDefinition> findByName(String name);

    boolean existsByName(String name);
}
