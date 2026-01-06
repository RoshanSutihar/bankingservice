package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.LoginCredentials;
import com.roshansutihar.bankingservice.repository.LoginCredentialsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoginCredentialsService {
//these codes are AI generated I used ChatGPT for this
    @Autowired
    private LoginCredentialsRepository loginCredentialsRepository;

    public List<LoginCredentials> getAllLoginCredentials() {
        return loginCredentialsRepository.findAll();
    }

    public Optional<LoginCredentials> getLoginCredentialsById(Long id) {
        return loginCredentialsRepository.findById(id);
    }

    public Optional<LoginCredentials> getLoginCredentialsByUserId(Long userId) {
        return loginCredentialsRepository.findByUserId(userId);
    }

    public LoginCredentials createLoginCredentials(LoginCredentials loginCredentials) {
        return loginCredentialsRepository.save(loginCredentials);
    }

    public LoginCredentials updateLoginCredentials(LoginCredentials loginCredentials) {
        return loginCredentialsRepository.save(loginCredentials);
    }

    public void deleteLoginCredentials(Long id) {
        loginCredentialsRepository.deleteById(id);
    }

    public void updateLastLogin(Long userId) {
        loginCredentialsRepository.findByUserId(userId).ifPresent(credentials -> {
            credentials.setLastLogin(LocalDateTime.now());
            loginCredentialsRepository.save(credentials);
        });
    }
}