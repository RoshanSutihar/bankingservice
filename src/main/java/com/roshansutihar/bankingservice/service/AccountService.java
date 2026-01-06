package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.entity.LedgerEntry;
import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.enums.AccountStatus;
import com.roshansutihar.bankingservice.enums.EntryType;
import com.roshansutihar.bankingservice.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public List<Account> getAccountsByUserIdWithAccountType(Long userId) {
        return accountRepository.findByUserIdWithAccountType(userId);
    }
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<Account> getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public List<Account> getAccountsByUserIdAndStatus(Long userId, AccountStatus status) {
        return accountRepository.findByUserIdAndStatus(userId, status);
    }

    public Account createAccount(Account account) {
        return accountRepository.save(account);
    }

    public Account updateAccount(Account account) {
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    public boolean accountNumberExists(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    public BigDecimal getAccountBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .map(Account::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getAccountBalanceByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(Account::getCurrentBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getAvailableBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .map(Account::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getAvailableBalanceByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(Account::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Autowired
    private LedgerEntryService ledgerEntryService;

    public void debitAccount(Account account, BigDecimal amount, String description, String ref, Transaction txn) {
        if (account == null || !AccountStatus.ACTIVE.equals(account.getStatus())) {
            throw new RuntimeException("Account inactive or invalid");
        }
        if (account.getCurrentBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        if (ref != null && (txn.getTransactionRef() == null || txn.getTransactionRef().isEmpty())) {
            txn.setTransactionRef(ref);
        }

        BigDecimal oldBalance = account.getCurrentBalance();
        account.setCurrentBalance(oldBalance.subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepository.save(account);

        LedgerEntry debitEntry = new LedgerEntry();
        debitEntry.setTransaction(txn);
        debitEntry.setAccount(account);
        debitEntry.setEntryType(EntryType.DEBIT);
        debitEntry.setAmount(amount);
        debitEntry.setRunningBalance(account.getCurrentBalance());
        ledgerEntryService.createLedgerEntry(debitEntry);
    }

    public void creditAccount(Account account, BigDecimal amount, String description, String ref, Transaction txn) {
        if (account == null || !AccountStatus.ACTIVE.equals(account.getStatus())) {
            throw new RuntimeException("Account inactive or invalid");
        }

        if (ref != null && (txn.getTransactionRef() == null || txn.getTransactionRef().isEmpty())) {
            txn.setTransactionRef(ref);
        }

        BigDecimal oldBalance = account.getCurrentBalance();
        account.setCurrentBalance(oldBalance.add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        accountRepository.save(account);

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setTransaction(txn);
        creditEntry.setAccount(account);
        creditEntry.setEntryType(EntryType.CREDIT);
        creditEntry.setAmount(amount);
        creditEntry.setRunningBalance(account.getCurrentBalance());
        ledgerEntryService.createLedgerEntry(creditEntry);
    }
}
