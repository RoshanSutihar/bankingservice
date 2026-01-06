package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;


    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }


}