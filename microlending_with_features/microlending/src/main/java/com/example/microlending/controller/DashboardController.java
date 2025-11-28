package com.example.microlending.controller;

import com.example.microlending.entity.Loan;
import com.example.microlending.entity.Payment;
import com.example.microlending.service.LoanService;
import com.example.microlending.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final LoanService loanService;
    private final PaymentService paymentService;

    public DashboardController(LoanService loanService, PaymentService paymentService) {
        this.loanService = loanService;
        this.paymentService = paymentService;
    }

    // 1️ Get all loans of a user
    @GetMapping("/loans/{userId}")
    public ResponseEntity<List<Loan>> getUserLoans(@PathVariable Long userId) {
        List<Loan> loans = loanService.findByUserId(userId);
        return ResponseEntity.ok(loans);
    }

    // 2️ Get all payments of a user
    @GetMapping("/payments/{userId}")
    public ResponseEntity<List<Payment>> getUserPayments(@PathVariable Long userId) {
        // This is the fix: Changed from findAll() to findByUserId()
        List<Payment> payments = paymentService.findByUserId(userId);
        return ResponseEntity.ok(payments);
    }

    // 3️ Get dashboard summary for a user
    @GetMapping("/summary/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDashboardSummary(@PathVariable Long userId) {
        List<Loan> loans = loanService.findByUserId(userId);

        List<Payment> payments = paymentService.findByUserId(userId);


        double totalLoanAmount = loans.stream()
                .filter(loan -> "APPROVED".equals(loan.getStatus()) || "CLOSED".equals(loan.getStatus()))
                .mapToDouble(loan -> loan.getTotalAmount() != null ? loan.getTotalAmount().doubleValue() : 0.0)
                .sum();

        double totalPaid = payments.stream()
                .filter(payment -> "SUCCESS".equals(payment.getStatus()))
                .mapToDouble(payment -> payment.getAmount() != null ? payment.getAmount().doubleValue() : 0.0)
                .sum();

        double outstanding = totalLoanAmount - totalPaid;

        Map<String, Object> summary = Map.of(
                "totalLoans", loans.size(),
                "totalLoanAmount", totalLoanAmount,
                "totalPaid", totalPaid,
                "outstandingAmount", outstanding
        );

        return ResponseEntity.ok(summary);
    }
}

