package com.afitnerd.tnra.repository;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PostState;
import com.afitnerd.tnra.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PostRepository extends CrudRepository<Post, Long> {

    public List<Post> findByUser(User user);
    public List<Post> findByUserAndState(User user, PostState state);
    public List<Post> findByUserAndStateOrderByFinishDesc(User user, PostState state);
}
