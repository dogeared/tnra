package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.StatDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatDefinitionRepository extends JpaRepository<StatDefinition, Long> {

    @Query("SELECT s FROM StatDefinition s WHERE TYPE(s) = StatDefinition AND s.archived = false ORDER BY s.displayOrder ASC")
    List<StatDefinition> findGlobalActiveOrderByDisplayOrderAsc();

    @Query("SELECT s FROM StatDefinition s WHERE TYPE(s) = StatDefinition ORDER BY s.displayOrder ASC")
    List<StatDefinition> findGlobalAllOrderByDisplayOrderAsc();

    @Query("SELECT COUNT(s) > 0 FROM StatDefinition s WHERE TYPE(s) = StatDefinition AND s.name = :name")
    boolean existsGlobalByName(@Param("name") String name);

    Optional<StatDefinition> findByName(String name);
}
