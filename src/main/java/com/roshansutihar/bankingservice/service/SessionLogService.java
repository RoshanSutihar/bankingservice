package com.roshansutihar.bankingservice.service;

import com.roshansutihar.bankingservice.entity.SessionLog;
import com.roshansutihar.bankingservice.repository.SessionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionLogService {

    @Autowired
    private SessionLogRepository sessionLogRepository;

    public List<SessionLog> getAllSessionLogs() {
        return sessionLogRepository.findAll();
    }

    public Optional<SessionLog> getSessionLogById(Long id) {
        return sessionLogRepository.findById(id);
    }

    public List<SessionLog> getSessionLogsByUserId(Long userId) {
        return sessionLogRepository.findByUserId(userId);
    }

    public Optional<SessionLog> getSessionLogByToken(String sessionToken) {
        return sessionLogRepository.findBySessionToken(sessionToken);
    }

    public List<SessionLog> getRecentSessions(LocalDateTime since) {
        return sessionLogRepository.findByLoginTimeAfter(since);
    }

    public SessionLog createSessionLog(SessionLog sessionLog) {
        return sessionLogRepository.save(sessionLog);
    }

    public SessionLog updateSessionLog(SessionLog sessionLog) {
        return sessionLogRepository.save(sessionLog);
    }

    public void deleteSessionLog(Long id) {
        sessionLogRepository.deleteById(id);
    }

    public void logoutSession(String sessionToken) {
        sessionLogRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setLogoutTime(LocalDateTime.now());
            sessionLogRepository.save(session);
        });
    }
}
