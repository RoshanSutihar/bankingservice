package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.LedgerEntry;
import com.roshansutihar.bankingservice.repository.LedgerEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class LedgerEntryService {

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    public List<LedgerEntry> getAllLedgerEntries() {
        return ledgerEntryRepository.findAll();
    }

    public Optional<LedgerEntry> getLedgerEntryById(Long id) {
        return ledgerEntryRepository.findById(id);
    }

    public List<LedgerEntry> getLedgerEntriesByAccountId(Long accountId) {
        return ledgerEntryRepository.findByAccountId(accountId);
    }

    public List<LedgerEntry> getLedgerEntriesByAccountIdOrderByDate(Long accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public List<LedgerEntry> getLedgerEntriesByTransactionId(Long transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId);
    }

    public LedgerEntry createLedgerEntry(LedgerEntry ledgerEntry) {
        return ledgerEntryRepository.save(ledgerEntry);
    }

    public LedgerEntry updateLedgerEntry(LedgerEntry ledgerEntry) {
        return ledgerEntryRepository.save(ledgerEntry);
    }

    public void deleteLedgerEntry(Long id) {
        ledgerEntryRepository.deleteById(id);
    }

    public BigDecimal getCurrentBalance(Long accountId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return entries.isEmpty() ? BigDecimal.ZERO : entries.get(0).getRunningBalance();
    }
}
