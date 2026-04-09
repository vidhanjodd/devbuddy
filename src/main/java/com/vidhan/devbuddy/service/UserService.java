package com.vidhan.devbuddy.service;

import com.vidhan.devbuddy.entity.User;
import com.vidhan.devbuddy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void registerUser(User user) {
        user.setRole("USER");
        userRepository.save(user);
    }
}