package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.Post;

public interface SlackNotificationService {
    void sendActivityNotification(Post post);
}
