package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findByUserId(Long userId);
    Optional<Business> findByTaxId(String taxId);
}
