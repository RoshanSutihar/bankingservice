package com.roshansutihar.bankingservice.email;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Value("${frontend.login-url}")
    private String loginUrl;

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

    public void sendTellerAccountCreationEmail(String toEmail,
                                               String username,
                                               String tempPassword,
                                               String accountNumber,
                                               String routingNumber) {
        String subject = "Your Bank Account Has Been Created - Login Details";

        String text = String.format(
                "Dear %s,\n\n" +
                        "A bank teller has successfully created your account.\n\n" +
                        "Login Details:\n" +
                        "Username: %s\n" +
                        "Temporary Password: %s\n\n" +
                        "IMPORTANT: You must change this password the first time you log in.\n\n" +
                        "Login here: %s\n\n" +
                        "Account Details:\n" +
                        "Account Number: %s\n" +
                        "Routing Number: %s\n\n" +
                        "If you did not request this account, please contact us immediately.\n\n" +
                        "Thank you,\n" +
                        "The Banking Team",
                username,
                username,
                tempPassword,
                loginUrl,           // ← Now comes from config
                accountNumber,
                routingNumber
        );

        sendSimpleMessage(toEmail, subject, text);
    }
}