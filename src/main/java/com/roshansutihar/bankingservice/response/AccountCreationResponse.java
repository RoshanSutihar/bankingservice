package com.roshansutihar.bankingservice.response;

public class AccountCreationResponse {
    private String accountNumber;
    private String routingNumber;
    private String username;
    private String message;

    public AccountCreationResponse() {}

    public AccountCreationResponse(String accountNumber, String routingNumber, String username, String message) {
        this.accountNumber = accountNumber;
        this.routingNumber = routingNumber;
        this.username = username;
        this.message = message;
    }


    public String getAccountNumber() { return accountNumber; }
    public String getRoutingNumber() { return routingNumber; }
    public String getUsername() { return username; }
    public String getMessage() { return message; }


    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setRoutingNumber(String routingNumber) { this.routingNumber = routingNumber; }
    public void setUsername(String username) { this.username = username; }
    public void setMessage(String message) { this.message = message; }
}