package com.roshansutihar.bankingservice.resource;

import com.roshansutihar.bankingservice.entity.Account;
import com.roshansutihar.bankingservice.entity.LedgerEntry;
import com.roshansutihar.bankingservice.entity.Transaction;
import com.roshansutihar.bankingservice.entity.TransactionType;
import com.roshansutihar.bankingservice.enums.AccountStatus;
import com.roshansutihar.bankingservice.enums.EntryType;
import com.roshansutihar.bankingservice.enums.TransactionStatus;
import com.roshansutihar.bankingservice.request.DepositRequest;
import com.roshansutihar.bankingservice.response.DepositResponse;
import com.roshansutihar.bankingservice.service.AccountService;
import com.roshansutihar.bankingservice.service.LedgerEntryService;
import com.roshansutihar.bankingservice.service.TransactionService;
import com.roshansutihar.bankingservice.service.TransactionTypeService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/teller")
public class TellerController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionTypeService transactionTypeService;

    @Autowired
    private LedgerEntryService ledgerService;


    @GetMapping("/deposit")
    public String showDepositForm(Model model) {
        model.addAttribute("depositRequest", new DepositRequest());
        return "teller-deposit";
    }


    @PostMapping("/deposit")
    public String processDeposit(@ModelAttribute DepositRequest request, Model model) {
        try {
            DepositResponse response = processDepositTransaction(request);
            model.addAttribute("success", true);
            model.addAttribute("transactionRef", response.getTransactionRef());
            model.addAttribute("amount", response.getAmount());
            model.addAttribute("newBalance", response.getNewBalance());
        } catch (Exception e) {
            model.addAttribute("error", "Deposit failed: " + e.getMessage());
        }
        model.addAttribute("depositRequest", new DepositRequest());
        return "teller-deposit";
    }

    @Transactional(rollbackOn = Exception.class)
    private DepositResponse processDepositTransaction(DepositRequest request) {

        Account account = accountService.getAccountByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));


        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active");
        }


        TransactionType depositType = transactionTypeService.getDepositTransactionType();


        Transaction transaction = new Transaction();
        transaction.setTransactionRef(generateTransactionRef());
        transaction.setToAccount(account);
        transaction.setTransactionType(depositType);
        transaction.setAmount(request.getAmount());
        transaction.setDescription("Cash deposit - " + request.getDescription());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setEffectiveDate(LocalDate.now());

        Transaction savedTransaction = transactionService.createTransaction(transaction);


        BigDecimal newBalance = account.getCurrentBalance().add(request.getAmount());
        account.setCurrentBalance(newBalance);
        account.setAvailableBalance(newBalance);
        accountService.updateAccount(account);


        LedgerEntry ledgerEntry = new LedgerEntry();
        ledgerEntry.setTransaction(savedTransaction);
        ledgerEntry.setAccount(account);
        ledgerEntry.setEntryType(EntryType.CREDIT);
        ledgerEntry.setAmount(request.getAmount());
        ledgerEntry.setRunningBalance(newBalance);
        ledgerService.createLedgerEntry(ledgerEntry);

        return new DepositResponse(
                savedTransaction.getTransactionRef(),
                account.getAccountNumber(),
                request.getAmount(),
                newBalance,
                "Deposit completed successfully"
        );
    }

    private String generateTransactionRef() {
        return "DEP" + System.currentTimeMillis();
    }
}
