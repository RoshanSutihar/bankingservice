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

@Controller
@RequestMapping("/accounts")
public class AccountController {

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


    @GetMapping("/create")
    public String showCreateAccountForm(Model model) {
        model.addAttribute("individualAccountRequest", new IndividualAccountRequest());
        return "create-account";
    }


    @PostMapping("/create")
    public String createAccount(@ModelAttribute IndividualAccountRequest request, Model model) {
        try {
            AccountCreationResponse response = createIndividualAccount(request);
            model.addAttribute("success", true);
            model.addAttribute("accountNumber", response.getAccountNumber());
            model.addAttribute("routingNumber", response.getRoutingNumber());
        } catch (Exception e) {
            model.addAttribute("error", "Account creation failed: " + e.getMessage());
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
        } catch (Exception e) {
            model.addAttribute("error", "Business account creation failed: " + e.getMessage());
        }
        return "create-business-account";
    }

    @Transactional(rollbackOn = Exception.class)
    private AccountCreationResponse createIndividualAccount(IndividualAccountRequest request) {

        // 1. Create user in Keycloak (admin API)
        String tempPassword = generateTemporaryPassword(); // e.g., random 12-char
        String keycloakUserId = keycloakAdminService.createUserInKeycloak(
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                tempPassword
        );

        // 2. Create local User linked to Keycloak
        User user = new User();
        user.setKeycloakSub(keycloakUserId);           // ← Critical link
        user.setUserType(UserType.INDIVIDUAL);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userService.createOrUpdateUser(user);

        // 3. Create profile
        Individual individual = new Individual();
        individual.setUser(savedUser);
        individual.setFirstName(request.getFirstName());
        individual.setLastName(request.getLastName());
        individual.setDateOfBirth(request.getDateOfBirth());
        individual.setSsn(request.getSsn());
        individual.setAddress(request.getAddress());
        individualService.createIndividual(individual);

        // 4. Create bank account
        AccountType accountType = accountTypeService.getCheckingAccountType();
        Account account = new Account();
        account.setUser(savedUser);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setRoutingNumber(routingNumber);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setOverdraftProtection(request.isOverdraftProtection());
        Account savedAccount = accountService.createAccount(account);

        // 5. Send email with login instructions + temp password
        emailService.sendTellerAccountCreationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                tempPassword,
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber()
        );

        return new AccountCreationResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber(),
                savedUser.getUsername(),
                "Account created successfully. Login details sent to customer."
        );
    }
    @Transactional(rollbackOn = Exception.class)
    private AccountCreationResponse createBusinessAccountInternal(BusinessAccountRequest request) {

        // 1. Generate temporary password
        String tempPassword = generateTemporaryPassword();

        // 2. Create user in Keycloak using Admin API
        // For business accounts, we use business name as "lastName" (or you can leave empty)
        String keycloakUserId = keycloakAdminService.createUserInKeycloak(
                request.getUsername(),
                request.getEmail(),
                "",  // firstName - optional for business
                request.getBusinessName(),  // use business name as lastName field in Keycloak
                tempPassword
        );

        // 3. Create local User record linked to Keycloak
        User user = new User();
        user.setKeycloakSub(keycloakUserId);           // ← Critical: links to Keycloak identity
        user.setUserType(UserType.BUSINESS);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userService.createOrUpdateUser(user);

        // 4. Create Business profile
        Business business = new Business();
        business.setUser(savedUser);
        business.setBusinessName(request.getBusinessName());
        business.setTaxId(request.getTaxId());
        business.setAddress(request.getAddress());
        businessService.createBusiness(business);

        // 5. Create bank account
        AccountType accountType = accountTypeService.getBusinessAccountType();
        Account account = new Account();
        account.setUser(savedUser);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setRoutingNumber(routingNumber);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setOverdraftProtection(request.isOverdraftProtection());
        Account savedAccount = accountService.createAccount(account);

        // 6. Send email with login instructions and temporary password
        emailService.sendTellerAccountCreationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                tempPassword,
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber()
        );

        // 7. Return success response
        return new AccountCreationResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber(),
                savedUser.getUsername(),
                "Business account created successfully. Login details have been sent to the provided email."
        );
    }

    private String generateTemporaryPassword() {
        // Generates a secure 12-character temporary password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    private String generateAccountNumber() {
        Random random = new Random();
        return String.format("01%09d", random.nextInt(1000000000));
    }
}