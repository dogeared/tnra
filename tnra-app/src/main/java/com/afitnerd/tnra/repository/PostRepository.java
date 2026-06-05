package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUser(User user);
    List<Post> findByUserAndState(User user, PostState state);
    List<Post> findByUserAndStateOrderByFinishDesc(User user, PostState state);
    Optional<Post> findFirstByUserAndStateOrderByFinishDesc(User user, PostState state);
    
    // Paginated query with sorting by start date descending
    Page<Post> findByUserOrderByFinishDesc(User user, Pageable pageable);
    
    // Paginated query for completed posts only, sorted by finish date descending
    Page<Post> findByUserAndStateOrderByFinishDesc(User user, PostState state, Pageable pageable);
}
