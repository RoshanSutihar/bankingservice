package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByActionType(String actionType);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
