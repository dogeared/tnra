package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalStatDefinitionRepository extends JpaRepository<PersonalStatDefinition, Long> {

    List<PersonalStatDefinition> findByUserAndArchivedFalseOrderByDisplayOrderAsc(User user);

    List<PersonalStatDefinition> findByUserOrderByDisplayOrderAsc(User user);

    List<PersonalStatDefinition> findByArchivedFalse();
}
