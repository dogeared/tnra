package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;

public interface VaadinPostService {
    Post finishPost(User me);
}
