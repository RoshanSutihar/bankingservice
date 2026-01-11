package com.roshansutihar.bankingservice.resource;

import com.roshansutihar.bankingservice.email.EmailService;
import com.roshansutihar.bankingservice.entity.*;

import com.roshansutihar.bankingservice.enums.AccountStatus;
import com.roshansutihar.bankingservice.enums.UserStatus;
import com.roshansutihar.bankingservice.enums.UserType;

import com.roshansutihar.bankingservice.request.BusinessAccountRequest;
import com.roshansutihar.bankingservice.request.IndividualAccountRequest;
import com.roshansutihar.bankingservice.response.AccountCreationResponse;
import com.roshansutihar.bankingservice.service.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Random;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private IndividualService individualService;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountTypeService accountTypeService;

    @Autowired
    private EmailService emailService;

    @Value("${payment.routing.number}")
    private String routingNumber;

    @Autowired
    private KeycloakAdminService keycloakAdminService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @GetMapping("/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("individualAccountRequest", new IndividualAccountRequest());
        model.addAttribute("success", false);
        return "create-account";
    }

    @PostMapping("/create")
    public String createAccount(@ModelAttribute IndividualAccountRequest request, Model model) {
        try {
            AccountCreationResponse response = createIndividualAccount(request);
            model.addAttribute("success", true);
            model.addAttribute("accountNumber", response.getAccountNumber());
            model.addAttribute("routingNumber", response.getRoutingNumber());
            model.addAttribute("message", response.getMessage());
        } catch (Exception e) {
            logger.error("Individual account creation failed: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("individualAccountRequest", request);
        }
        return "create-account";
    }

    @GetMapping("/create-business")
    public String showCreateBusinessAccountForm(Model model) {
        model.addAttribute("businessAccountRequest", new BusinessAccountRequest());
        model.addAttribute("success", false);
        return "create-business-account";
    }

    @PostMapping("/create-business")
    public String createBusinessAccount(@ModelAttribute BusinessAccountRequest request, Model model) {
        try {
            AccountCreationResponse response = createBusinessAccountInternal(request);
            model.addAttribute("success", true);
            model.addAttribute("accountNumber", response.getAccountNumber());
            model.addAttribute("routingNumber", response.getRoutingNumber());
            model.addAttribute("message", response.getMessage());
        } catch (Exception e) {
            logger.error("Business account creation failed: {}", e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("success", false);
            model.addAttribute("businessAccountRequest", request);
        }
        return "create-business-account";
    }

    private AccountCreationResponse createIndividualAccount(IndividualAccountRequest request) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("IndividualAccountCreation");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(def);
        String keycloakUserId = null;
        String tempPassword = null;

        try {
            logger.info("Starting individual account creation for username: {}", request.getUsername());

            // 1. VALIDATION: Check if user already exists in our system
            User existingUser = userService.getUserByUsername(request.getUsername());
            if (existingUser != null) {
                throw new RuntimeException("Username '" + request.getUsername() + "' is already taken. Please choose a different username.");
            }

            // 2. Generate temporary password
            tempPassword = generateTemporaryPassword();
            logger.debug("Generated temporary password for user: {}", request.getUsername());

            // 3. Create user in Keycloak
            keycloakUserId = keycloakAdminService.createUserInKeycloak(
                    request.getUsername(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    tempPassword
            );
            logger.info("Created Keycloak user with ID: {}", keycloakUserId);

            // 4. Create user in our database
            User user = new User();
            user.setKeycloakSub(keycloakUserId);
            user.setUserType(UserType.INDIVIDUAL);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setStatus(UserStatus.ACTIVE);
            User savedUser = userService.createOrUpdateUser(user);
            logger.debug("Created database user with ID: {}", savedUser.getId());

            // 5. Create individual profile
            Individual individual = new Individual();
            individual.setUser(savedUser);
            individual.setFirstName(request.getFirstName());
            individual.setLastName(request.getLastName());
            individual.setDateOfBirth(request.getDateOfBirth());
            individual.setSsn(request.getSsn());
            individual.setAddress(request.getAddress());
            individualService.createIndividual(individual);
            logger.debug("Created individual profile for user: {}", savedUser.getUsername());

            // 6. Create account
            AccountType accountType = accountTypeService.getCheckingAccountType();
            String accountNumber = generateAccountNumber();

            Account account = new Account();
            account.setUser(savedUser);
            account.setAccountType(accountType);
            account.setAccountNumber(accountNumber);
            account.setRoutingNumber(routingNumber);
            account.setCurrentBalance(BigDecimal.ZERO);
            account.setAvailableBalance(BigDecimal.ZERO);
            account.setStatus(AccountStatus.ACTIVE);
            account.setOverdraftProtection(request.isOverdraftProtection());
            Account savedAccount = accountService.createAccount(account);
            logger.info("Created account with number: {} for user: {}", accountNumber, savedUser.getUsername());

            // 7. Send email notification
            emailService.sendTellerAccountCreationEmail(
                    savedUser.getEmail(),
                    savedUser.getUsername(),
                    tempPassword,
                    savedAccount.getAccountNumber(),
                    savedAccount.getRoutingNumber()
            );
            logger.debug("Sent account creation email to: {}", savedUser.getEmail());

            // 8. Commit transaction
            transactionManager.commit(status);
            logger.info("Successfully completed individual account creation for: {}", request.getUsername());

            return new AccountCreationResponse(
                    savedAccount.getAccountNumber(),
                    savedAccount.getRoutingNumber(),
                    savedUser.getUsername(),
                    "Account created successfully. Login details have been sent to your email."
            );

        } catch (Exception e) {
            // ROLLBACK LOGIC
            logger.error("Error during individual account creation. Rolling back transaction.", e);
            transactionManager.rollback(status);

            // Clean up Keycloak user if it was created
            if (keycloakUserId != null) {
                try {
                    logger.info("Cleaning up orphaned Keycloak user: {}", keycloakUserId);
                    // You need to add this method to your KeycloakAdminService
                    keycloakAdminService.deleteUserInKeycloak(keycloakUserId);
                    logger.info("Successfully cleaned up Keycloak user: {}", keycloakUserId);
                } catch (Exception cleanupEx) {
                    logger.error("Failed to cleanup Keycloak user: {}", keycloakUserId, cleanupEx);
                    // Don't throw this exception - the original error is more important
                }
            }

            // Check for duplicate key constraint violation
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("duplicate key") ||
                            errorMessage.contains("constraint") ||
                            errorMessage.contains("already exists"))) {
                errorMessage = "This username is already registered. Please try a different one.";
            } else {
                errorMessage = "Account creation failed: " + errorMessage;
            }

            throw new RuntimeException(errorMessage);
        }
    }

    private AccountCreationResponse createBusinessAccountInternal(BusinessAccountRequest request) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("BusinessAccountCreation");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(def);
        String keycloakUserId = null;
        String tempPassword = null;

        try {
            logger.info("Starting business account creation for username: {}", request.getUsername());

            // 1. VALIDATION: Check if user already exists
            User existingUser = userService.getUserByUsername(request.getUsername());
            if (existingUser != null) {
                throw new RuntimeException("Username '" + request.getUsername() + "' is already taken. Please choose a different username.");
            }

            // 2. Generate temporary password
            tempPassword = generateTemporaryPassword();
            logger.debug("Generated temporary password for business user: {}", request.getUsername());

            // 3. Create user in Keycloak
            keycloakUserId = keycloakAdminService.createUserInKeycloak(
                    request.getUsername(),
                    request.getEmail(),
                    "Business",  // firstName for business accounts
                    request.getBusinessName(),
                    tempPassword
            );
            logger.info("Created Keycloak business user with ID: {}", keycloakUserId);

            // 4. Create user in our database
            User user = new User();
            user.setKeycloakSub(keycloakUserId);
            user.setUserType(UserType.BUSINESS);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setStatus(UserStatus.ACTIVE);
            User savedUser = userService.createOrUpdateUser(user);
            logger.debug("Created database business user with ID: {}", savedUser.getId());

            // 5. Create business profile
            Business business = new Business();
            business.setUser(savedUser);
            business.setBusinessName(request.getBusinessName());
            business.setTaxId(request.getTaxId());
            business.setAddress(request.getAddress());
            businessService.createBusiness(business);
            logger.debug("Created business profile: {}", request.getBusinessName());

            // 6. Create business account
            AccountType accountType = accountTypeService.getBusinessAccountType();
            String accountNumber = generateAccountNumber();

            Account account = new Account();
            account.setUser(savedUser);
            account.setAccountType(accountType);
            account.setAccountNumber(accountNumber);
            account.setRoutingNumber(routingNumber);
            account.setCurrentBalance(BigDecimal.ZERO);
            account.setAvailableBalance(BigDecimal.ZERO);
            account.setStatus(AccountStatus.ACTIVE);
            account.setOverdraftProtection(request.isOverdraftProtection());
            Account savedAccount = accountService.createAccount(account);
            logger.info("Created business account with number: {} for: {}", accountNumber, request.getBusinessName());

            // 7. Send email notification
            emailService.sendTellerAccountCreationEmail(
                    savedUser.getEmail(),
                    savedUser.getUsername(),
                    tempPassword,
                    savedAccount.getAccountNumber(),
                    savedAccount.getRoutingNumber()
            );
            logger.debug("Sent business account creation email to: {}", savedUser.getEmail());

            // 8. Commit transaction
            transactionManager.commit(status);
            logger.info("Successfully completed business account creation for: {}", request.getBusinessName());

            return new AccountCreationResponse(
                    savedAccount.getAccountNumber(),
                    savedAccount.getRoutingNumber(),
                    savedUser.getUsername(),
                    "Business account created successfully. Login details have been sent to the provided email."
            );

        } catch (Exception e) {
            // ROLLBACK LOGIC
            logger.error("Error during business account creation. Rolling back transaction.", e);
            transactionManager.rollback(status);

            // Clean up Keycloak user if it was created
            if (keycloakUserId != null) {
                try {
                    logger.info("Cleaning up orphaned Keycloak business user: {}", keycloakUserId);
                    // You need to add this method to your KeycloakAdminService
                    keycloakAdminService.deleteUserInKeycloak(keycloakUserId);
                    logger.info("Successfully cleaned up Keycloak business user: {}", keycloakUserId);
                } catch (Exception cleanupEx) {
                    logger.error("Failed to cleanup Keycloak business user: {}", keycloakUserId, cleanupEx);
                }
            }

            // Check for duplicate key constraint violation
            String errorMessage = e.getMessage();
            if (errorMessage != null &&
                    (errorMessage.contains("duplicate key") ||
                            errorMessage.contains("constraint") ||
                            errorMessage.contains("already exists"))) {
                errorMessage = "This username is already registered. Please try a different one.";
            } else {
                errorMessage = "Business account creation failed: " + errorMessage;
            }

            throw new RuntimeException(errorMessage);
        }
    }

    private String generateTemporaryPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*";
        String allChars = upperCase + lowerCase + numbers + specialChars;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);

        // Ensure at least one character from each category
        sb.append(upperCase.charAt(random.nextInt(upperCase.length())));
        sb.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        sb.append(numbers.charAt(random.nextInt(numbers.length())));
        sb.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill remaining characters
        for (int i = 4; i < 12; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the characters
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }

    private String generateAccountNumber() {
        Random random = new Random();
        // Generate 9-digit number with leading zeros if needed
        String randomPart = String.format("%09d", random.nextInt(1000000000));
        // Prefix with bank identifier (e.g., "01" for your bank)
        return "01" + randomPart;
    }
}