package com.example.microlending.controller;

import com.example.microlending.entity.Payment;
import com.example.microlending.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * New controller to handle admin-specific payment actions.
 * This is mapped to "/api/admin/payments" and is automatically
 * secured by the "ADMIN" role in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * This is the endpoint your frontend is looking for.
     * It calls the service method that uses a "JOIN FETCH"
     * to include all Loan and User data, solving all "N/A" problems.
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        // This method must exist in your PaymentService and
        // call the 'findAllWithDetails' query in your PaymentRepository
        return ResponseEntity.ok(paymentService.findAllWithDetails());
    }
}

