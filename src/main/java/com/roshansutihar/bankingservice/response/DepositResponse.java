package com.roshansutihar.bankingservice.response;

import java.math.BigDecimal;

public class DepositResponse {
    private String transactionRef;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private String message;

    public DepositResponse(String transactionRef, String accountNumber, BigDecimal amount,
                           BigDecimal newBalance, String message) {
        this.transactionRef = transactionRef;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.newBalance = newBalance;
        this.message = message;
    }


    public String getTransactionRef() { return transactionRef; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getNewBalance() { return newBalance; }
    public String getMessage() { return message; }
}
