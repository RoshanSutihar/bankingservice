package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import com.roshansutihar.bankingservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public Optional<Transaction> getTransactionByRef(String transactionRef) {
        return transactionRepository.findByTransactionRef(transactionRef);
    }

    public List<Transaction> getTransactionsByFromAccountId(Long fromAccountId) {
        return transactionRepository.findByFromAccountId(fromAccountId);
    }

    public List<Transaction> getTransactionsByToAccountId(Long toAccountId) {
        return transactionRepository.findByToAccountId(toAccountId);
    }

    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId);
    }

    public List<Transaction> getTransactionsByEffectiveDate(LocalDate effectiveDate) {
        return transactionRepository.findByEffectiveDate(effectiveDate);
    }

    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }

    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }
}
