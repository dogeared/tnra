package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;
import com.afitnerd.tnra.model.User;

public interface EMailService {

    public void sendMailToMe(User user, Post post);
}
