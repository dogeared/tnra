package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.PersonalStatDefinition;
import com.afitnerd.tnra.model.StatDefinition;
import com.afitnerd.tnra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface VaadinPostPresenter {
    Post finishPost(User me);
    Optional<Post> getOptionalInProgressPost(User me);
    User initializeUser();
    Page<Post> getCompletedPostsPage(User me, Pageable pageable);
    Post startPost(User me);
    Post savePost(Post post);
    Post updateStatValue(StatDefinition statDef, Integer value);
    List<StatDefinition> getActiveGlobalStatDefinitions();
    List<PersonalStatDefinition> getActivePersonalStatDefinitions(User user);
    List<User> getAllActiveUsers();
}
