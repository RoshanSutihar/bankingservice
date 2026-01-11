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

    public AccountType getCheckingAccountType() {
        return accountTypeRepository.findByTypeCode("CHECKING")
                .orElseThrow(() -> new RuntimeException("CHECKING account type not found"));
    }

    public AccountType getBusinessAccountType() {
        return accountTypeRepository.findByTypeCode("BUSINESS_CHECKING")
                .orElseThrow(() -> new RuntimeException("BUSINESS account type not found"));
    }
}
