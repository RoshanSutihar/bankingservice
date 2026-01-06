package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByUserId(Long userId);
    List<Account> findByUserIdAndStatus(Long userId, AccountStatus status);
    boolean existsByAccountNumber(String accountNumber);
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.accountType WHERE a.user.id = :userId")
    List<Account> findByUserIdWithAccountType(@Param("userId") Long userId);
}
