package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.StatDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatDefinitionRepository extends JpaRepository<StatDefinition, Long> {

    @Query("SELECT s FROM StatDefinition s WHERE TYPE(s) = StatDefinition AND s.archived = false ORDER BY s.displayOrder ASC")
    List<StatDefinition> findGlobalActiveOrderByDisplayOrderAsc();

    @Query("SELECT s FROM StatDefinition s WHERE TYPE(s) = StatDefinition ORDER BY s.displayOrder ASC")
    List<StatDefinition> findGlobalAllOrderByDisplayOrderAsc();
}
