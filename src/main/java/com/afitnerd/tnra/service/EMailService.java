package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;

public interface EMailService {

    void sendMailToMe(User user, Post post);
    void sendMailToAll(Post post);
    void sendTextViaMail(User user, Post post);
}
