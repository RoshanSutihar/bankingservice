package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.AccountType;
import com.roshansutihar.bankingservice.repository.AccountTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountTypeService {

    @Autowired
    private AccountTypeRepository accountTypeRepository;

    public List<AccountType> getAllAccountTypes() {
        return accountTypeRepository.findAll();
    }

    public Optional<AccountType> getAccountTypeById(Long id) {
        return accountTypeRepository.findById(id);
    }

    public Optional<AccountType> getAccountTypeByCode(String typeCode) {
        return accountTypeRepository.findByTypeCode(typeCode);
    }

    public AccountType createAccountType(AccountType accountType) {
        return accountTypeRepository.save(accountType);
    }

    public AccountType updateAccountType(AccountType accountType) {
        return accountTypeRepository.save(accountType);
    }

    public void deleteAccountType(Long id) {
        accountTypeRepository.deleteById(id);
    }


    public AccountType getCheckingAccountType() {
        return accountTypeRepository.findByTypeCode("CHECKING")
                .orElseThrow(() -> new RuntimeException("CHECKING account type not found"));
    }

    public AccountType getSavingsAccountType() {
        return accountTypeRepository.findByTypeCode("SAVINGS")
                .orElseThrow(() -> new RuntimeException("SAVINGS account type not found"));
    }

    public AccountType getBusinessAccountType() {
        return accountTypeRepository.findByTypeCode("BUSINESS")
                .orElseThrow(() -> new RuntimeException("BUSINESS account type not found"));
    }
}
