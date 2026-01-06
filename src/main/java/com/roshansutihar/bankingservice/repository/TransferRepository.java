package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.Transfer;
import com.roshansutihar.bankingservice.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    List<Transfer> findByFromAccountId(Long fromAccountId);
    List<Transfer> findByStatus(TransferStatus status);
    List<Transfer> findByNextTransferDate(LocalDate nextTransferDate);
    List<Transfer> findByStatusAndNextTransferDateLessThanEqual(TransferStatus status, LocalDate date);
}
