package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.LoginCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginCredentialsRepository extends JpaRepository<LoginCredentials, Long> {
    Optional<LoginCredentials> findByUserId(Long userId);
}
