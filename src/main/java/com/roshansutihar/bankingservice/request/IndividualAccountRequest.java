package com.roshansutihar.bankingservice.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class IndividualAccountRequest {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String ssn;
    private String address;
    private boolean overdraftProtection;


    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isOverdraftProtection() { return overdraftProtection; }
    public void setOverdraftProtection(boolean overdraftProtection) { this.overdraftProtection = overdraftProtection; }
}