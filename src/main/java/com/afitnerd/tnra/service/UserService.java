package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;

public interface UserService {
    User getUserByEmail(String email);
    User getCurrentUser();
    User saveUser(User user);
    User getOrCreateUserFromOidc(String email, String firstName, String lastName);
    void deleteUser(User user);
}
