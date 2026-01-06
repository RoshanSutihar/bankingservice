package com.roshansutihar.bankingservice.resource;

import com.roshansutihar.bankingservice.request.DepositRequest;
import com.roshansutihar.bankingservice.request.IndividualAccountRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {


    @GetMapping("/")
    public String showHomePage() {
        return "home";
    }


    @GetMapping("/create-account")
    public String redirectToAccountCreation(Model model) {
        model.addAttribute("individualAccountRequest", new IndividualAccountRequest());
        return "create-account";
    }


    @GetMapping("/teller-deposit")
    public String redirectToTellerDeposit(Model model) {
        model.addAttribute("depositRequest", new DepositRequest());
        return "teller-deposit";
    }
}
