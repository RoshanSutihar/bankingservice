package com.roshansutihar.bankingservice.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAccountRequest {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String businessName;
    private String taxId;
    private String address;
    private boolean overdraftProtection;
}
