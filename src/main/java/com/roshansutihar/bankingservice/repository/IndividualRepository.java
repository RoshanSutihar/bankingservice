package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.Individual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndividualRepository extends JpaRepository<Individual, Long> {
    Optional<Individual> findByUserId(Long userId);
    Optional<Individual> findBySsn(String ssn);
}