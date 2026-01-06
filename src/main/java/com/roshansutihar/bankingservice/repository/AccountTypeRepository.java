package com.roshansutihar.bankingservice.repository;

import com.roshansutihar.bankingservice.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    Optional<AccountType> findByTypeCode(String typeCode);
}
