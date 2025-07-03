package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;

public interface UserService {
    User getUserByEmail(String email);
}
