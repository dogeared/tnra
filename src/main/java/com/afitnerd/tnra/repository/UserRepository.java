package com.afitnerd.tnra.repository;


import com.afitnerd.tnra.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {

    User findBySlackUserId(String slackUserId);
    User findBySlackUsername(String slackUsername);
    User findByEmail(String email);
    Iterable<User> findByActiveTrue();
    List<User> findByActiveTrueOrderByFirstName();
}
