package com.roshansutihar.bankingservice.entity;

import com.roshansutihar.bankingservice.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "accountType", "outgoingTransactions", "incomingTransactions", "ledgerEntries"})
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountType accountType;

    @Column(name = "routing_number", nullable = false, length = 20)
    private String routingNumber;

    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "opened_date")
    private LocalDateTime openedDate;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "overdraft_protection")
    private Boolean overdraftProtection = false;

    @Column(name = "overdraft_limit", precision = 10, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fromAccount")
    private List<Transaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount")
    private List<Transaction> incomingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();
}