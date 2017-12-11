package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;

public interface PostService {

    Post startPost(User user);
    Post finishPost(Post post);
    Post getInProgressPost(User user);

    Post replaceStats(User user, Stats stats);
    Post replaceIntro(User user, Intro intro);
    Post replacePersonal(User user, Category personal);
    Post replaceFamily(User user, Category personal);
    Post replaceWork(User user, Category personal);
}
