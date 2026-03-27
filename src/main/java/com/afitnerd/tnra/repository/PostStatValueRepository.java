package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostStatValue;
import com.afitnerd.tnra.model.StatDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostStatValueRepository extends JpaRepository<PostStatValue, Long> {

    List<PostStatValue> findByPost(Post post);

    Optional<PostStatValue> findByPostAndStatDefinition(Post post, StatDefinition statDefinition);

    List<PostStatValue> findByPostOrderByStatDefinitionDisplayOrderAsc(Post post);
}
