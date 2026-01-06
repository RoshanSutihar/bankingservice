package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountId(Long fromAccountId);
    List<Transaction> findByToAccountId(Long toAccountId);
    List<Transaction> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);
    Optional<Transaction> findByTransactionRef(String transactionRef);
    List<Transaction> findByEffectiveDate(LocalDate effectiveDate);
    List<Transaction> findByStatus(TransactionStatus status);
}