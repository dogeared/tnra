package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Category;
import com.afitnerd.tnra.model.Intro;
import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.Stats;
import com.afitnerd.tnra.model.User;

public interface PostService {

    Post startPost(User user);
    Post finishPost(User user);
    Post getInProgressPost(User user);
    Post savePost(Post post);

    Post replaceStats(User user, Stats stats);
    Post replaceIntro(User user, Intro intro);
    Post replacePersonal(User user, Category personal);
    Post replaceFamily(User user, Category family);
    Post replaceWork(User user, Category work);

    Post updateIntro(User user, Intro intro);
    Post updatePersonal(User user, Category personal);
    Post updateFamily(User user, Category family);
    Post updateWork(User user, Category work);
}
