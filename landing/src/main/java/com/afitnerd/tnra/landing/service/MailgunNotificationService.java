package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;

public interface MailgunNotificationService {
    void notifyFounder(RequestAccess request);
}
