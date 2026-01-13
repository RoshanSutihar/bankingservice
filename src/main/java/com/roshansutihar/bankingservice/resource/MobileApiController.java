package com.roshansutihar.bankingservice.resource;

import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.entity.Transaction;

import com.roshansutihar.bankingservice.entity.TransactionType;
import com.roshansutihar.bankingservice.entity.User;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import com.roshansutihar.bankingservice.enums.UserStatus;
import com.roshansutihar.bankingservice.enums.UserType;
import com.roshansutihar.bankingservice.service.AccountService;
import com.roshansutihar.bankingservice.service.TransactionService;
import com.roshansutihar.bankingservice.service.TransactionTypeService;
import com.roshansutihar.bankingservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/mobile")
public class MobileApiController {

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

    // Helper method to get/create user
    private User getOrCreateUser(OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        if (username == null || username.trim().isEmpty()) {
            username = oidcUser.getSubject();
        }

        User currentUser = userService.getUserByUsername(username);
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setUsername(username);
            currentUser.setKeycloakSub(oidcUser.getSubject());
            currentUser.setEmail(oidcUser.getEmail());
            currentUser.setCreatedAt(LocalDateTime.now());
            currentUser.setUserType(UserType.INDIVIDUAL);
            currentUser.setStatus(UserStatus.ACTIVE);
            currentUser = userService.createOrUpdateUser(currentUser);
        }
        return currentUser;
    }

    // 1. Dashboard API
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (oidcUser == null) {
                response.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User currentUser = getOrCreateUser(oidcUser);
            List<Account> accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());

            // Prepare user info
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", currentUser.getId());
            userInfo.put("username", currentUser.getUsername());
            userInfo.put("email", currentUser.getEmail());
            response.put("user", userInfo);

            // Prepare accounts
            List<Map<String, Object>> accountsList = new ArrayList<>();
            BigDecimal totalBalance = BigDecimal.ZERO;

            for (Account account : accounts) {
                Map<String, Object> accountInfo = new HashMap<>();
                accountInfo.put("id", account.getId());
                accountInfo.put("accountNumber", account.getAccountNumber());
                accountInfo.put("currentBalance", account.getCurrentBalance());
                accountInfo.put("availableBalance", account.getAvailableBalance());
                accountInfo.put("accountType", account.getAccountType().getTypeName());
                accountInfo.put("status", account.getStatus().toString());
                accountInfo.put("routingNumber", account.getRoutingNumber());
                accountInfo.put("openedDate", account.getOpenedDate());
                accountsList.add(accountInfo);

                if (account.getCurrentBalance() != null) {
                    totalBalance = totalBalance.add(account.getCurrentBalance());
                }
            }

            response.put("accounts", accountsList);
            response.put("totalBalance", totalBalance);
            response.put("accountCount", accounts.size());

            // Get recent transactions
            List<Transaction> recentTransactions = new ArrayList<>();
            for (Account account : accounts) {
                List<Transaction> accountTransactions = transactionService.getTransactionsByAccountId(account.getId());
                recentTransactions.addAll(accountTransactions);
            }

            // Sort by date (most recent first)
            recentTransactions.sort((t1, t2) -> {
                if (t1.getTransactionDate() == null && t2.getTransactionDate() == null) return 0;
                if (t1.getTransactionDate() == null) return 1;
                if (t2.getTransactionDate() == null) return -1;
                return t2.getTransactionDate().compareTo(t1.getTransactionDate());
            });

            // Limit to 10
            if (recentTransactions.size() > 10) {
                recentTransactions = recentTransactions.subList(0, 10);
            }

            // Prepare transactions
            List<Map<String, Object>> transactionsList = new ArrayList<>();
            for (Transaction txn : recentTransactions) {
                Map<String, Object> txnInfo = new HashMap<>();
                txnInfo.put("id", txn.getId());
                txnInfo.put("amount", txn.getAmount());
                txnInfo.put("description", txn.getDescription());
                txnInfo.put("transactionDate", txn.getTransactionDate());
                txnInfo.put("status", txn.getStatus().toString());
                txnInfo.put("transactionRef", txn.getTransactionRef());
                txnInfo.put("effectiveDate", txn.getEffectiveDate());
                txnInfo.put("transactionType", txn.getTransactionType().getTypeCode());

                // Add account info
                if (txn.getFromAccount() != null) {
                    txnInfo.put("fromAccount", txn.getFromAccount().getAccountNumber());
                }
                if (txn.getToAccount() != null) {
                    txnInfo.put("toAccount", txn.getToAccount().getAccountNumber());
                }

                transactionsList.add(txnInfo);
            }

            response.put("recentTransactions", transactionsList);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to load dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 2. Your existing QR validation endpoint - fixed to match your entities
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/validate-qr")
    public ResponseEntity<Map<String, Object>> validateQR(@RequestBody Map<String, String> request,
                                                          @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
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

            User currentUser = getOrCreateUser(oidcUser);
            List<Account> accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());

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
            response.put("payerName", currentUser.getUsername());
            response.put("payerAccountBalance", primaryAccount.getCurrentBalance());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", "Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 3. Your existing process-payment endpoint - fixed
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/process-payment")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> request,
                                                              @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
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

            User currentUser = getOrCreateUser(oidcUser);

            Optional<Account> optPayerAccount = accountService.getAccountById(payerAccountId);
            if (!optPayerAccount.isPresent()) {
                response.put("success", false);
                response.put("error", "Invalid account");
                return ResponseEntity.badRequest().body(response);
            }

            Account payerAccount = optPayerAccount.get();
            if (!payerAccount.getUser().getId().equals(currentUser.getId())) {
                response.put("success", false);
                response.put("error", "Unauthorized account access");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Optional<TransactionType> paymentTransactionTypeOpt = transactionTypeService.getTransactionTypeByCode("PAYMENT");
            if (!paymentTransactionTypeOpt.isPresent()) {
                response.put("success", false);
                response.put("error", "Payment transaction type not found");
                return ResponseEntity.badRequest().body(response);
            }

            TransactionType paymentTransactionType = paymentTransactionTypeOpt.get();

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
            response.put("transactionId", txn.getId());
            response.put("paymentReference", completionBody.get("transactionId"));
            response.put("settlementBatchId", settlementBatchId);
            response.put("message", "Payment completed — funds routed to merchant settlement batch");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 4. Get account details
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Map<String, Object>> getAccountDetails(@PathVariable Long accountId,
                                                                 @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getOrCreateUser(oidcUser);

            Optional<Account> accountOpt = accountService.getAccountById(accountId);
            if (!accountOpt.isPresent()) {
                response.put("error", "Account not found");
                return ResponseEntity.notFound().build();
            }

            Account account = accountOpt.get();
            if (!account.getUser().getId().equals(currentUser.getId())) {
                response.put("error", "Unauthorized");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Get transactions for this account
            List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);

            // Sort by date (most recent first)
            transactions.sort((t1, t2) -> {
                if (t1.getTransactionDate() == null && t2.getTransactionDate() == null) return 0;
                if (t1.getTransactionDate() == null) return 1;
                if (t2.getTransactionDate() == null) return -1;
                return t2.getTransactionDate().compareTo(t1.getTransactionDate());
            });

            Map<String, Object> accountInfo = new HashMap<>();
            accountInfo.put("id", account.getId());
            accountInfo.put("accountNumber", account.getAccountNumber());
            accountInfo.put("currentBalance", account.getCurrentBalance());
            accountInfo.put("availableBalance", account.getAvailableBalance());
            accountInfo.put("accountType", account.getAccountType().getTypeName());
            accountInfo.put("status", account.getStatus().toString());
            accountInfo.put("routingNumber", account.getRoutingNumber());
            accountInfo.put("openedDate", account.getOpenedDate());
            accountInfo.put("overdraftProtection", account.getOverdraftProtection());
            accountInfo.put("overdraftLimit", account.getOverdraftLimit());

            List<Map<String, Object>> transactionsList = new ArrayList<>();
            for (Transaction txn : transactions) {
                Map<String, Object> txnInfo = new HashMap<>();
                txnInfo.put("id", txn.getId());
                txnInfo.put("amount", txn.getAmount());
                txnInfo.put("description", txn.getDescription());
                txnInfo.put("transactionDate", txn.getTransactionDate());
                txnInfo.put("status", txn.getStatus().toString());
                txnInfo.put("transactionRef", txn.getTransactionRef());
                txnInfo.put("effectiveDate", txn.getEffectiveDate());
                txnInfo.put("transactionType", txn.getTransactionType().getTypeCode());

                if (txn.getFromAccount() != null) {
                    txnInfo.put("fromAccount", txn.getFromAccount().getAccountNumber());
                }
                if (txn.getToAccount() != null) {
                    txnInfo.put("toAccount", txn.getToAccount().getAccountNumber());
                }

                transactionsList.add(txnInfo);
            }

            response.put("account", accountInfo);
            response.put("transactions", transactionsList);
            response.put("transactionCount", transactions.size());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5. Get all transactions with pagination
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OidcUser oidcUser) {

        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getOrCreateUser(oidcUser);
            List<Account> accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());

            List<Transaction> allTransactions = new ArrayList<>();
            for (Account account : accounts) {
                List<Transaction> accountTransactions = transactionService.getTransactionsByAccountId(account.getId());
                allTransactions.addAll(accountTransactions);
            }

            // Sort by date (most recent first)
            allTransactions.sort((t1, t2) -> {
                if (t1.getTransactionDate() == null && t2.getTransactionDate() == null) return 0;
                if (t1.getTransactionDate() == null) return 1;
                if (t2.getTransactionDate() == null) return -1;
                return t2.getTransactionDate().compareTo(t1.getTransactionDate());
            });

            // Pagination logic
            int total = allTransactions.size();
            int start = Math.min(page * size, total);
            int end = Math.min((page + 1) * size, total);

            List<Transaction> pagedTransactions = allTransactions.subList(start, end);

            // Prepare response
            List<Map<String, Object>> transactionsList = new ArrayList<>();
            for (Transaction txn : pagedTransactions) {
                Map<String, Object> txnInfo = new HashMap<>();
                txnInfo.put("id", txn.getId());
                txnInfo.put("amount", txn.getAmount());
                txnInfo.put("description", txn.getDescription());
                txnInfo.put("transactionDate", txn.getTransactionDate());
                txnInfo.put("status", txn.getStatus().toString());
                txnInfo.put("transactionRef", txn.getTransactionRef());
                txnInfo.put("effectiveDate", txn.getEffectiveDate());
                txnInfo.put("transactionType", txn.getTransactionType().getTypeCode());

                if (txn.getFromAccount() != null) {
                    txnInfo.put("fromAccount", txn.getFromAccount().getAccountNumber());
                }
                if (txn.getToAccount() != null) {
                    txnInfo.put("toAccount", txn.getToAccount().getAccountNumber());
                }

                transactionsList.add(txnInfo);
            }

            response.put("transactions", transactionsList);
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalTransactions", total);
            response.put("totalPages", (int) Math.ceil((double) total / size));
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}