package com.roshansutihar.bankingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_code", unique = true, nullable = false, length = 20)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    private String description;

    @Column(name = "affects_balance")
    private Boolean affectsBalance = true;

    @OneToMany(mappedBy = "transactionType")
    private List<Transaction> transactions = new ArrayList<>();
}