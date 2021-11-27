package com.afitnerd.tnra.service.pq;

import com.afitnerd.tnra.model.User;

public interface PQRefreshService {

    void refreshAuth(User user);
    void refreshAuthAll();
}
