package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.GoToGuySet;
import org.springframework.data.repository.CrudRepository;

public interface GoToGuySetRepository extends CrudRepository<GoToGuySet, Long> {

    GoToGuySet findTopByOrderByStartDateDesc();
}
