package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    List<SessionLog> findByUserId(Long userId);
    Optional<SessionLog> findBySessionToken(String sessionToken);
    List<SessionLog> findByLoginTimeAfter(LocalDateTime time);
}