package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccountId(Long accountId);
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    List<LedgerEntry> findByTransactionId(Long transactionId);
}