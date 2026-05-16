package com.afitnerd.tnra.landing.service;

import com.afitnerd.tnra.landing.model.RequestAccess;

public interface RequestAccessService {
    boolean isRateLimited(String ipAddress);
    RequestAccess submit(RequestAccess request);
}
