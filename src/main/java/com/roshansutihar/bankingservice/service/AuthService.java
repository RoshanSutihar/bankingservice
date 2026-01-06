package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.LoginCredentials;
import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    //these codes are AI generated I used ChatGPT for this
    @Autowired
    private LoginCredentialsService loginCredentialsService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private UserRepository userRepository;

    public void createUserCredentials(Long userId, String password) {
        String salt = passwordService.generateSalt();
        String hashedPassword = passwordService.hashPassword(password, salt);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LoginCredentials credentials = new LoginCredentials();
        credentials.setUser(user);
        credentials.setPasswordHash(hashedPassword);
        credentials.setPasswordSalt(salt);

        loginCredentialsService.createLoginCredentials(credentials);
    }

    public boolean authenticateUser(Long userId, String password) {
        Optional<LoginCredentials> credentials = loginCredentialsService.getLoginCredentialsByUserId(userId);
        if (credentials.isPresent()) {
            LoginCredentials creds = credentials.get();
            boolean isAuthenticated = passwordService.verifyPassword(password, creds.getPasswordSalt(), creds.getPasswordHash());

            if (isAuthenticated) {
                loginCredentialsService.updateLastLogin(userId);
            }

            return isAuthenticated;
        }
        return false;
    }
}