package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.Transfer;
import com.roshansutihar.bankingservice.enums.TransferStatus;
import com.roshansutihar.bankingservice.repository.TransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    @Autowired
    private TransferRepository transferRepository;

    public List<Transfer> getAllTransfers() {
        return transferRepository.findAll();
    }

    public Optional<Transfer> getTransferById(Long id) {
        return transferRepository.findById(id);
    }

    public List<Transfer> getTransfersByFromAccountId(Long fromAccountId) {
        return transferRepository.findByFromAccountId(fromAccountId);
    }

    public List<Transfer> getTransfersByStatus(TransferStatus status) {
        return transferRepository.findByStatus(status);
    }

    public List<Transfer> getTransfersByNextTransferDate(LocalDate nextTransferDate) {
        return transferRepository.findByNextTransferDate(nextTransferDate);
    }

    public List<Transfer> getPendingTransfersDue(LocalDate date) {
        return transferRepository.findByStatusAndNextTransferDateLessThanEqual(TransferStatus.ACTIVE, date);
    }

    public Transfer createTransfer(Transfer transfer) {
        return transferRepository.save(transfer);
    }

    public Transfer updateTransfer(Transfer transfer) {
        return transferRepository.save(transfer);
    }

    public void deleteTransfer(Long id) {
        transferRepository.deleteById(id);
    }
}
