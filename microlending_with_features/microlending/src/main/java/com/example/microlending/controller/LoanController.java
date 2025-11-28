package com.example.microlending.controller;

import com.example.microlending.entity.Installmentdetails;
import com.example.microlending.entity.Loan;
import com.example.microlending.entity.User;
import com.example.microlending.service.LoanService;
import com.example.microlending.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final UserService userService;

    public LoanController(LoanService loanService, UserService userService) {
        this.loanService = loanService;
        this.userService = userService;
    }


    @PostMapping("/request")
    public ResponseEntity<?> requestLoan(@RequestParam Long userId,
                                         @RequestParam Double amount,
                                         @RequestParam String purpose,
                                         @RequestParam("document") MultipartFile document,
                                         @RequestParam("document2") MultipartFile document2) { // <-- Added document2
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }


        Loan loan = loanService.requestLoan(userOpt.get(), amount, purpose, document, document2);
        return ResponseEntity.ok(loan);
    }

    // Get installment schedule of a loan
    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<Installmentdetails>> getInstallments(@PathVariable Long loanId) {
        List<Installmentdetails> installments = loanService.generateInstallments(loanId);
        return ResponseEntity.ok(installments);
    }

    // ️ Get all loans of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Loan>> getLoansByUser(@PathVariable Long userId) {
        List<Loan> loans = loanService.findByUserId(userId);
        return ResponseEntity.ok(loans);
    }
}

