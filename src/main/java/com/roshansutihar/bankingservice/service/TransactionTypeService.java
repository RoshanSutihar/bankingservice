package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.TransactionType;
import com.roshansutihar.bankingservice.repository.TransactionTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionTypeService {

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    public List<TransactionType> getAllTransactionTypes() {
        return transactionTypeRepository.findAll();
    }

    public Optional<TransactionType> getTransactionTypeById(Long id) {
        return transactionTypeRepository.findById(id);
    }

    public Optional<TransactionType> getTransactionTypeByCode(String typeCode) {
        return transactionTypeRepository.findByTypeCode(typeCode);
    }

    public TransactionType createTransactionType(TransactionType transactionType) {
        return transactionTypeRepository.save(transactionType);
    }

    public TransactionType updateTransactionType(TransactionType transactionType) {
        return transactionTypeRepository.save(transactionType);
    }

    public void deleteTransactionType(Long id) {
        transactionTypeRepository.deleteById(id);
    }

    public TransactionType getDepositTransactionType() {
        return transactionTypeRepository.findByTypeCode("DEPOSIT")
                .orElseThrow(() -> new RuntimeException("DEPOSIT transaction type not found"));
    }

    public TransactionType getWithdrawalTransactionType() {
        return transactionTypeRepository.findByTypeCode("WITHDRAWAL")
                .orElseThrow(() -> new RuntimeException("WITHDRAWAL transaction type not found"));
    }

    public TransactionType getTransferTransactionType() {
        return transactionTypeRepository.findByTypeCode("TRANSFER")
                .orElseThrow(() -> new RuntimeException("TRANSFER transaction type not found"));
    }

}
