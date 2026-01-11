package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public User createOrUpdateUser(User user) {
        // Try to find by keycloakSub (most reliable identifier from Keycloak)
        Optional<User> existing = userRepository.findByKeycloakSub(user.getKeycloakSub());

        if (existing.isPresent()) {
            User found = existing.get();
            found.setUsername(user.getUsername());
            found.setEmail(user.getEmail());
            return userRepository.save(found);
        } else {
            user.setCreatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }
    }


    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return userRepository.findByKeycloakSub(keycloakSub);
    }

    public User getUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }
}