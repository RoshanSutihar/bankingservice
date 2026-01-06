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
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    @Value("${payment.routing.number}")
    private String routingNumber;


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

        User user = new User();
        user.setUserType(UserType.INDIVIDUAL);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userService.createUser(user);


        Individual individual = new Individual();
        individual.setUser(savedUser);
        individual.setFirstName(request.getFirstName());
        individual.setLastName(request.getLastName());
        individual.setDateOfBirth(request.getDateOfBirth());
        individual.setSsn(request.getSsn());
        individual.setAddress(request.getAddress());
        individualService.createIndividual(individual);


        authService.createUserCredentials(savedUser.getId(), request.getPassword());


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

        try {
            emailService.sendAccountCreationEmail(
                    savedUser.getEmail(),
                    savedUser.getUsername(),
                    savedAccount.getAccountNumber(),
                    savedAccount.getRoutingNumber()
            );
        } catch (Exception e) {

            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return new AccountCreationResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber(),
                savedUser.getUsername(),
                "Account created successfully"
        );
    }
    @Transactional(rollbackOn = Exception.class)
    private AccountCreationResponse createBusinessAccountInternal(BusinessAccountRequest request) {

        User user = new User();
        user.setUserType(UserType.BUSINESS);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userService.createUser(user);


        Business business = new Business();
        business.setUser(savedUser);
        business.setBusinessName(request.getBusinessName());
        business.setTaxId(request.getTaxId());
        business.setAddress(request.getAddress());
        businessService.createBusiness(business);


        authService.createUserCredentials(savedUser.getId(), request.getPassword());


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



        return new AccountCreationResponse(
                savedAccount.getAccountNumber(),
                savedAccount.getRoutingNumber(),
                savedUser.getUsername(),
                "Business account created successfully"
        );
    }

    private String generateAccountNumber() {
        Random random = new Random();
        return String.format("01%09d", random.nextInt(1000000000));
    }
}