package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.AuditLog;
import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.repository.AuditLogRepository;
import com.roshansutihar.bankingservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    public Optional<AuditLog> getAuditLogById(Long id) {
        return auditLogRepository.findById(id);
    }

    public List<AuditLog> getAuditLogsByUserId(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> getAuditLogsByActionType(String actionType) {
        return auditLogRepository.findByActionType(actionType);
    }

    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetween(start, end);
    }

    public AuditLog createAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    public void logUserAction(Long userId, String actionType, String tableName, Long recordId, String newValues) {
        AuditLog auditLog = new AuditLog();

        if (userId != null) {
            User user = new User();
            user.setId(userId);
            auditLog.setUser(user);
        }

        auditLog.setActionType(actionType);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setNewValues(newValues);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }

    public void deleteAuditLog(Long id) {
        auditLogRepository.deleteById(id);
    }
}