package com.roshansutihar.bankingservice.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.entity.TransactionType;
import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import com.roshansutihar.bankingservice.enums.TransferStatus;
import com.roshansutihar.bankingservice.enums.UserStatus;
import com.roshansutihar.bankingservice.enums.UserType;
import com.roshansutihar.bankingservice.service.AccountService;
import com.roshansutihar.bankingservice.service.TransactionService;
import com.roshansutihar.bankingservice.service.TransactionTypeService;
import com.roshansutihar.bankingservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
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

            if (oidcUser == null) {
                System.err.println("ERROR: OidcUser is null!");
                model.addAttribute("error", "User not authenticated");
                return "error";
            }

            // Get the most reliable identifier
            String username = oidcUser.getPreferredUsername();
            if (username == null || username.trim().isEmpty()) {
                username = oidcUser.getSubject(); // fallback to Keycloak user UUID
                System.out.println("Using subject as username: " + username);
            }


            // Get or create user
            User currentUser = null;
            try {
                currentUser = userService.getUserByUsername(username);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }

            if (currentUser == null) {
                currentUser = new User();
                currentUser.setUsername(username);
                currentUser.setKeycloakSub(oidcUser.getSubject());
                currentUser.setEmail(oidcUser.getEmail());
                currentUser.setCreatedAt(LocalDateTime.now());
                currentUser.setUserType(UserType.INDIVIDUAL);
                currentUser.setStatus(UserStatus.ACTIVE);
                try {
                    currentUser = userService.createOrUpdateUser(currentUser);
                } catch (Exception e) {
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
    public ResponseEntity<Map<String, Object>> validateQR(@RequestBody Map<String, String> request, @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            // First validate the authentication
            if (oidcUser == null) {
                response.put("valid", false);
                response.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

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

            // Get payment session details from payments core
            PaymentSessionData sessionData = getPaymentSessionData(sessionId);
            if (sessionData == null || !sessionData.isValid()) {
                response.put("valid", false);
                response.put("error", "Invalid or expired payment session");
                return ResponseEntity.badRequest().body(response);
            }

            // CRITICAL SECURITY FIX: Validate ALL values against payment core data
            validatePaymentParameters(sessionData, amount, currency, merchantId, transactionRef);

            // Use the same logic as dashboard() to get username
            String username = oidcUser.getPreferredUsername();
            if (username == null || username.trim().isEmpty()) {
                username = oidcUser.getSubject();
            }

            System.out.println("QR Validation - Looking for user with username: " + username);

            // Get user with proper error handling
            User currentUser = null;
            try {
                currentUser = userService.getUserByUsername(username);
                System.out.println("QR Validation - User found: " + (currentUser != null));
            } catch (Exception e) {
                System.err.println("QR Validation - ERROR in getUserByUsername: " + e.getMessage());
                e.printStackTrace();
                response.put("valid", false);
                response.put("error", "User lookup failed: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            // If user doesn't exist, create them (same as dashboard logic)
            if (currentUser == null) {
                System.out.println("QR Validation - Creating new user...");
                currentUser = new User();
                currentUser.setUsername(username);
                currentUser.setKeycloakSub(oidcUser.getSubject());
                currentUser.setEmail(oidcUser.getEmail());
                currentUser.setCreatedAt(LocalDateTime.now());
                currentUser.setUserType(UserType.INDIVIDUAL);
                currentUser.setStatus(UserStatus.ACTIVE);

                try {
                    currentUser = userService.createOrUpdateUser(currentUser);
                    System.out.println("QR Validation - New user created with ID: " + currentUser.getId());
                } catch (Exception e) {
                    System.err.println("QR Validation - ERROR creating user: " + e.getMessage());
                    e.printStackTrace();
                    response.put("valid", false);
                    response.put("error", "Failed to create user: " + e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Now safely get accounts
            List<Account> accounts = Collections.emptyList();
            try {
                accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());
                System.out.println("QR Validation - Found accounts: " + accounts.size());
            } catch (Exception e) {
                System.err.println("QR Validation - ERROR fetching accounts: " + e.getMessage());
                response.put("valid", false);
                response.put("error", "Failed to fetch accounts: " + e.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            if (accounts.isEmpty()) {
                response.put("valid", false);
                response.put("error", "No account found");
                return ResponseEntity.badRequest().body(response);
            }

            Account primaryAccount = accounts.get(0);
            if (primaryAccount.getCurrentBalance().compareTo(amount) < 0) {
                response.put("valid", false);
                response.put("error", "Insufficient balance");
                response.put("requiredAmount", amountDouble);
                response.put("currentBalance", primaryAccount.getCurrentBalance());
                return ResponseEntity.badRequest().body(response);
            }

            response.put("valid", true);
            response.put("sessionId", sessionId);
            response.put("amount", amountDouble);
            response.put("currency", currency);
            response.put("merchantId", merchantId);
            response.put("transactionRef", transactionRef);
            response.put("payerAccountId", primaryAccount.getId());
            response.put("merchantName", sessionData.getMerchantName());
            response.put("payerAccountNumber", primaryAccount.getAccountNumber());
            response.put("payerName", currentUser.getUsername());

            // Store the validated session data for later verification
            response.put("verifiedAmount", sessionData.getAmount().doubleValue());
            response.put("verifiedTransactionRef", sessionData.getTransactionRef());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("QR validation error: " + e.getMessage());
            e.printStackTrace();
            response.put("valid", false);
            response.put("error", "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/process-payment")
    @Transactional
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request, @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate authentication
            if (oidcUser == null) {
                response.put("success", false);
                response.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String sessionId = (String) request.get("sessionId");
            double amountDouble = (Double) request.get("amount");
            BigDecimal amount = BigDecimal.valueOf(amountDouble);
            String merchantId = (String) request.get("merchantId");
            String transactionRef = (String) request.get("transactionRef");
            Long payerAccountId = ((Number) request.get("payerAccountId")).longValue();

            // CRITICAL SECURITY FIX: Re-validate against payment core before processing
            PaymentSessionData sessionData = getPaymentSessionData(sessionId);
            if (sessionData == null || !sessionData.isValid()) {
                throw new RuntimeException("Invalid or expired payment session");
            }

            // Validate all parameters match the payment session
            validatePaymentParameters(sessionData, amount, sessionData.getCurrency(), merchantId, transactionRef);

            // Get username using same logic
            String username = oidcUser.getPreferredUsername();
            if (username == null || username.trim().isEmpty()) {
                username = oidcUser.getSubject();
            }

            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                response.put("success", false);
                response.put("error", "User not found");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Account> optPayerAccount = accountService.getAccountById(payerAccountId);
            Account payerAccount = optPayerAccount.orElseThrow(() -> new RuntimeException("Invalid account"));
            if (!payerAccount.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Unauthorized account access");
            }

            // Verify balance using the validated amount from payment core
            if (payerAccount.getCurrentBalance().compareTo(sessionData.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
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

    // Helper method to get payment session data from payments core
    private PaymentSessionData getPaymentSessionData(String sessionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    paymentsCoreUrl + "/api/v1/payments/status/" + sessionId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                return PaymentSessionData.builder()
                        .sessionId(sessionId)
                        .amount(new BigDecimal(body.get("amount").toString()))
                        .currency((String) body.get("currency"))
                        .merchantId((String) body.get("merchantId"))
                        .transactionRef((String) body.get("transactionRef"))
                        .status(TransactionStatus.valueOf((String) body.get("status")))
                        .expiryTime(LocalDateTime.parse((String) body.get("expiryTime")))
                        .isValid(true)
                        .build();
            }
        } catch (Exception e) {
            System.err.println("Failed to get payment session data: " + e.getMessage());
        }
        return null;
    }

    // Helper method to validate payment parameters against payment core data
    private void validatePaymentParameters(PaymentSessionData sessionData,
                                           BigDecimal amount,
                                           String currency,
                                           String merchantId,
                                           String transactionRef) {
        // Check if session is expired
        if (sessionData.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Payment session has expired");
        }

        // Check if session is already completed or cancelled
        if (sessionData.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Payment session is no longer valid (status: " + sessionData.getStatus() + ")");
        }

        // Validate amount matches exactly (use compareTo for BigDecimal)
        if (amount.compareTo(sessionData.getAmount()) != 0) {
            throw new RuntimeException("Amount mismatch. Expected: " + sessionData.getAmount() + ", Received: " + amount);
        }

        // Validate merchant ID
        if (!merchantId.equals(sessionData.getMerchantId())) {
            throw new RuntimeException("Merchant ID mismatch");
        }

        // Validate transaction reference
        if (!transactionRef.equals(sessionData.getTransactionRef())) {
            throw new RuntimeException("Transaction reference mismatch");
        }

        // Validate currency
        if (!currency.equalsIgnoreCase(sessionData.getCurrency())) {
            throw new RuntimeException("Currency mismatch. Expected: " + sessionData.getCurrency() + ", Received: " + currency);
        }
    }

    // Inner class to hold payment session data
    @Data
    @Builder
    private static class PaymentSessionData {
        private String sessionId;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String transactionRef;
        private TransactionStatus status;
        private LocalDateTime expiryTime;
        private boolean isValid;

        public String getMerchantName() {
            return merchantId;
        }
    }
}