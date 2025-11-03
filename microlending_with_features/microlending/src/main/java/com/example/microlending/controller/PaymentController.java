package com.example.microlending.controller;

import com.example.microlending.entity.Payment;
import com.example.microlending.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 1️⃣ Initiate payment (installment or lump-sum)
    @PostMapping("/initiate")
    public ResponseEntity<Payment> initiatePayment(@RequestParam Long loanId,
                                                   @RequestParam(required = false) Long installmentId,
                                                   @RequestParam double amount) {
        Payment payment = paymentService.initiateDummyPayment(loanId, installmentId, amount);
        return ResponseEntity.ok(payment);
    }

    // 2️⃣ Verify payment (and send receipt)
    @PostMapping("/verify")
    public ResponseEntity<Payment> verifyPayment(@RequestParam Long paymentId,
                                                 @RequestParam boolean success) {
        Payment payment = paymentService.verifyDummyPayment(paymentId, success);
        return ResponseEntity.ok(payment);
    }
}
