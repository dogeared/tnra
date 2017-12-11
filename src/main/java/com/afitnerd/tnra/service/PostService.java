package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;

public interface PostService {

    Post startPost(User user);
    void finishPost(Post post);
    Post getInProgressPost(User user);

    Post updateStats(User user, Stats stats);
    Post updateIntro(User user, Intro intro);
    void updatePersonal(User user, Category personal);
    void updateFamnily(User user, Category personal);
    void updateWork(User user, Category personal);
}
