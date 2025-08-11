package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface VaadinPostService {
    Post finishPost(User me);
    Optional<Post> getOptionalInProgressPost(User me);
    User initializeUser();
    Page<Post> getCompletedPostsPage(User me, Pageable pageable);
    Post startPost(User me);
    Post savePost(Post post);
    Post updateCompleteStats(Stats stats);
}
