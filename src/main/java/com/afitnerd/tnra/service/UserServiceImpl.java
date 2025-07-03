package com.afitnerd.tnra.service;

import com.afitnerd.tnra.repository.UserRepository;

import org.springframework.stereotype.Service;

import com.afitnerd.tnra.model.User;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

}
