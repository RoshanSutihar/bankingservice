package com.roshansutihar.bankingservice.email;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;



    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply.emailservices.rs@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    public void sendAccountCreationEmail(String toEmail, String username, String accountNumber, String routingNumber) {
        String subject = "Welcome to Our Bank - Account Created Successfully";
        String text = String.format(
                "Dear %s,\n\n" +
                        "Your bank account has been created successfully!\n\n" +
                        "Account Details:\n" +
                        "Account Number: %s\n" +
                        "Routing Number: %s\n\n" +
                        "Please keep this information secure.\n\n" +
                        "Thank you for choosing our bank!\n\n" +
                        "Best regards,\n" +
                        "Banking Team",
                username, accountNumber, routingNumber
        );

        sendSimpleMessage(toEmail, subject, text);
    }

    public void sendDepositConfirmationEmail(String toEmail, String accountNumber, BigDecimal amount, BigDecimal newBalance, String transactionRef) {
        String subject = "Deposit Confirmation - Transaction #" + transactionRef;
        String text = String.format(
                "Dear Customer,\n\n" +
                        "A deposit has been made to your account.\n\n" +
                        "Transaction Details:\n" +
                        "Account Number: %s\n" +
                        "Amount Deposited: $%.2f\n" +
                        "New Balance: $%.2f\n" +
                        "Transaction Reference: %s\n\n" +
                        "If you did not authorize this transaction, please contact us immediately.\n\n" +
                        "Thank you,\n" +
                        "Banking Team",
                accountNumber, amount, newBalance, transactionRef
        );

        sendSimpleMessage(toEmail, subject, text);
    }

    public void sendLowBalanceAlert(String toEmail, String accountNumber, BigDecimal currentBalance) {
        String subject = "Low Balance Alert - Account #" + accountNumber;
        String text = String.format(
                "Dear Customer,\n\n" +
                        "Your account balance is getting low.\n\n" +
                        "Account: %s\n" +
                        "Current Balance: $%.2f\n\n" +
                        "Please consider depositing funds to avoid any service charges.\n\n" +
                        "Thank you,\n" +
                        "Banking Team",
                accountNumber, currentBalance
        );

        sendSimpleMessage(toEmail, subject, text);
    }
}