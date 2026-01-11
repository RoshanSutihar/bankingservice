package com.roshansutihar.bankingservice.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.entity.TransactionType;
import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import com.roshansutihar.bankingservice.service.AccountService;
import com.roshansutihar.bankingservice.service.TransactionService;
import com.roshansutihar.bankingservice.service.TransactionTypeService;
import com.roshansutihar.bankingservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionTypeService transactionTypeService;

    @Value("${payment.routing.number}")
    private String sourceRoutingNumber;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${payments.core.base-url}")
    private String paymentsCoreUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String dashboard(Model model, @AuthenticationPrincipal OidcUser oidcUser, HttpServletRequest request) {
        try {
            // Debug: Print oidcUser info
            System.out.println("=== DEBUG: DashboardController called ===");
            System.out.println("OidcUser is null: " + (oidcUser == null));

            if (oidcUser == null) {
                System.err.println("ERROR: OidcUser is null!");
                model.addAttribute("error", "User not authenticated");
                return "error";
            }

            // Debug: Print all claims
            System.out.println("Subject: " + oidcUser.getSubject());
            System.out.println("PreferredUsername: " + oidcUser.getPreferredUsername());
            System.out.println("Email: " + oidcUser.getEmail());

            // Get the most reliable identifier
            String username = oidcUser.getPreferredUsername();
            if (username == null || username.trim().isEmpty()) {
                username = oidcUser.getSubject(); // fallback to Keycloak user UUID
                System.out.println("Using subject as username: " + username);
            }

            System.out.println("Looking for user with username: " + username);

            // Get or create user
            User currentUser = null;
            try {
                currentUser = userService.getUserByUsername(username);
                System.out.println("User found: " + (currentUser != null));
            } catch (Exception e) {
                System.err.println("ERROR in getUserByUsername: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            if (currentUser == null) {
                System.out.println("Creating new user...");
                currentUser = new User();
                currentUser.setUsername(username);
                currentUser.setKeycloakSub(oidcUser.getSubject());
                currentUser.setEmail(oidcUser.getEmail());
                currentUser.setCreatedAt(LocalDateTime.now());

                try {
                    currentUser = userService.createOrUpdateUser(currentUser);
                    System.out.println("New user created with ID: " + currentUser.getId());
                } catch (Exception e) {
                    System.err.println("ERROR creating user: " + e.getMessage());
                    e.printStackTrace();
                    model.addAttribute("error", "Failed to create user: " + e.getMessage());
                    return "error";
                }
            }

            // Debug: Check user ID
            System.out.println("Current user ID: " + currentUser.getId());

            if (currentUser.getId() == null) {
                System.err.println("ERROR: User ID is null!");
                model.addAttribute("error", "User has no ID");
                return "error";
            }

            // Get accounts
            List<Account> accounts = Collections.emptyList();
            try {
                accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());
                System.out.println("Found accounts: " + accounts.size());
            } catch (Exception e) {
                System.err.println("ERROR fetching accounts: " + e.getMessage());
                e.printStackTrace();
                // Continue with empty accounts list
            }

            // Prepare model data
            model.addAttribute("user", currentUser);
            model.addAttribute("accounts", accounts);
            model.addAttribute("noAccountsYet", accounts.isEmpty());

            // Add total balance
            BigDecimal totalBalance = accounts.stream()
                    .map(Account::getCurrentBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            model.addAttribute("totalBalance", totalBalance);

            // Get transactions for each account
            List<Transaction> recentTransactions = new ArrayList<>();
            for (Account account : accounts) {
                try {
                    List<Transaction> accountTransactions = transactionService.getTransactionsByAccountId(account.getId());
                    // Take up to 5 most recent transactions from all accounts
                    recentTransactions.addAll(accountTransactions);
                } catch (Exception e) {
                    System.err.println("ERROR fetching transactions for account " + account.getId() + ": " + e.getMessage());
                }
            }

            // Sort by transaction date (most recent first) and limit to 10
            recentTransactions.sort((t1, t2) -> {
                if (t1.getTransactionDate() == null && t2.getTransactionDate() == null) return 0;
                if (t1.getTransactionDate() == null) return 1;
                if (t2.getTransactionDate() == null) return -1;
                return t2.getTransactionDate().compareTo(t1.getTransactionDate());
            });

            // Limit to 10 most recent
            if (recentTransactions.size() > 10) {
                recentTransactions = recentTransactions.subList(0, 10);
            }

            // FIX: Add transactions to model with BOTH variable names
            model.addAttribute("recentTransactions", recentTransactions);
            model.addAttribute("allTransactions", recentTransactions); // This fixes the template error

            System.out.println("=== DEBUG: DashboardController finished successfully ===");
            return "mobilebank-dashboard";

        } catch (Exception e) {
            System.err.println("FATAL ERROR in dashboard(): " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Internal server error: " + e.getMessage());
            model.addAttribute("timestamp", LocalDateTime.now());
            return "error";
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/validate-qr")
    public ResponseEntity<Map<String, Object>> validateQR(@RequestBody Map<String, String> request, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String qrData = request.get("qrData");
            if (qrData == null || !qrData.startsWith("QRPAY|")) {
                response.put("valid", false);
                response.put("error", "Invalid QR format");
                return ResponseEntity.badRequest().body(response);
            }

            String[] parts = qrData.split("\\|");
            if (parts.length != 6) {
                response.put("valid", false);
                response.put("error", "Malformed QR data");
                return ResponseEntity.badRequest().body(response);
            }

            String sessionId = parts[1];
            double amountDouble = Double.parseDouble(parts[2]);
            BigDecimal amount = BigDecimal.valueOf(amountDouble);
            String currency = parts[3];
            String merchantId = parts[4];
            String transactionRef = parts[5];

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> paymentsResponse = restTemplate.exchange(
                    paymentsCoreUrl + "/api/v1/payments/verify/" + sessionId, HttpMethod.GET, entity, Map.class);

            if (!paymentsResponse.getStatusCode().is2xxSuccessful() || paymentsResponse.getBody() == null) {
                response.put("valid", false);
                response.put("error", "Session validation failed");
                return ResponseEntity.badRequest().body(response);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> sessionData = paymentsResponse.getBody();
            if (!(Boolean) sessionData.getOrDefault("valid", false)) {
                response.put("valid", false);
                response.put("error", sessionData.getOrDefault("message", "Invalid session"));
                return ResponseEntity.badRequest().body(response);
            }

            if (!merchantId.equals(sessionData.get("merchantId"))) {
                response.put("valid", false);
                response.put("error", "Merchant mismatch");
                return ResponseEntity.badRequest().body(response);
            }

            User currentUser = userService.getUserByUsername(principal.getName());
            List<Account> accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());
            Account primaryAccount = accounts.isEmpty() ? null : accounts.get(0);
            if (primaryAccount == null) {
                response.put("valid", false);
                response.put("error", "No account found");
                return ResponseEntity.badRequest().body(response);
            }
            if (primaryAccount.getCurrentBalance().compareTo(amount) < 0) {
                response.put("valid", false);
                response.put("error", "Insufficient balance");
                response.put("requiredAmount", amountDouble);
                return ResponseEntity.badRequest().body(response);
            }

            String merchantName = (String) sessionData.getOrDefault("merchantName", merchantId);

            response.put("valid", true);
            response.put("sessionId", sessionId);
            response.put("amount", amountDouble);
            response.put("currency", currency);
            response.put("merchantId", merchantId);
            response.put("transactionRef", transactionRef);
            response.put("payerAccountId", primaryAccount.getId());
            response.put("merchantName", merchantName);
            response.put("payerAccountNumber", primaryAccount.getAccountNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("QR validation error: " + e.getMessage());
            e.printStackTrace();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/process-payment")
    @Transactional
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request, Principal principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sessionId = (String) request.get("sessionId");
            double amountDouble = (Double) request.get("amount");
            BigDecimal amount = BigDecimal.valueOf(amountDouble);
            String merchantId = (String) request.get("merchantId");
            String transactionRef = (String) request.get("transactionRef");
            Long payerAccountId = ((Number) request.get("payerAccountId")).longValue();

            User currentUser = userService.getUserByUsername(principal.getName());
            Optional<Account> optPayerAccount = accountService.getAccountById(payerAccountId);
            Account payerAccount = optPayerAccount.orElseThrow(() -> new RuntimeException("Invalid account"));
            if (!payerAccount.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized account access");
            }

            TransactionType paymentTransactionType = transactionTypeService.getTransactionTypeByCode("PAYMENT")
                    .orElseThrow(() -> new RuntimeException("Payment transaction type not found"));

            Transaction txn = new Transaction();
            txn.setFromAccount(payerAccount);
            txn.setToAccount(null);
            txn.setAmount(amount.negate());
            txn.setDescription("QR Payment to " + merchantId);
            txn.setTransactionRef(transactionRef);
            txn.setEffectiveDate(LocalDate.now());
            txn.setTransactionType(paymentTransactionType);
            txn.setStatus(TransactionStatus.PENDING);
            txn.setTransactionDate(LocalDateTime.now());

            transactionService.createTransaction(txn);

            String description = "QR Payment to " + merchantId;
            accountService.debitAccount(payerAccount, amount, description, transactionRef, txn);

            Map<String, Object> completionPayload = new HashMap<>();
            completionPayload.put("sessionId", sessionId);
            completionPayload.put("fromAccount", payerAccount.getAccountNumber());
            completionPayload.put("merchantId", merchantId);
            completionPayload.put("grossAmount", amountDouble);
            completionPayload.put("transactionRef", transactionRef);
            completionPayload.put("payerUserId", currentUser.getId());
            completionPayload.put("sourceRoutingNumber", sourceRoutingNumber);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(completionPayload, headers);

            ResponseEntity<Map> completionResponse = restTemplate.postForEntity(
                    paymentsCoreUrl + "/api/v1/payments/complete",
                    entity,
                    Map.class
            );

            if (!completionResponse.getStatusCode().is2xxSuccessful()) {
                txn.setStatus(TransactionStatus.FAILED);
                transactionService.updateTransaction(txn);
                throw new RuntimeException("PaymentCore notification failed: " + completionResponse.getStatusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> completionBody = completionResponse.getBody();
            if (completionBody == null || !Boolean.TRUE.equals(completionBody.get("success"))) {
                txn.setStatus(TransactionStatus.FAILED);
                transactionService.updateTransaction(txn);
                throw new RuntimeException("PaymentCore rejected: " +
                        (completionBody != null ? completionBody.get("message") : "Unknown error"));
            }

            String settlementBatchId = (String) completionBody.get("settlementBatchId");

            txn.setStatus(TransactionStatus.PENDING);
            transactionService.updateTransaction(txn);

            response.put("success", true);
            response.put("transactionId", txn.getId().toString());
            response.put("paymentReference", completionBody.get("transactionId"));
            response.put("settlementBatchId", settlementBatchId);
            response.put("message", "Payment completed — funds routed to merchant settlement batch");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Payment processing error: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}