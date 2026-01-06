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
import org.springframework.security.oauth2.jwt.Jwt;
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
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String dashboard(Model model, @AuthenticationPrincipal OidcUser oidcUser, HttpServletRequest request) {

        // --- USER INFO ---
        String username = oidcUser.getPreferredUsername();
        if (username == null) {
            username = oidcUser.getSubject();
        }

        User currentUser = userService.getUserByUsername(username);

        List<Account> accounts = accountService.getAccountsByUserIdWithAccountType(currentUser.getId());

        List<Transaction> allTransactions = new ArrayList<>();
        for (Account acc : accounts) {
            allTransactions.addAll(transactionService.getTransactionsByAccountId(acc.getId()));
        }

        allTransactions.sort(Comparator.comparing(Transaction::getEffectiveDate).reversed());

        List<Transaction> recentTransactions = allTransactions.stream()
                .limit(10)
                .collect(Collectors.toList());

        // --- ADD TO MODEL ---
        model.addAttribute("accounts", accounts);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("allTransactions", allTransactions);
        model.addAttribute("user", currentUser);

        return "mobilebank-dashboard";
    }


    @Autowired
    private RestTemplate restTemplate;

    @Value("${payments.core.base-url}")
    private String paymentsCoreUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

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