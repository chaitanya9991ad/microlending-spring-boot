package com.example.microlending.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentid;

    private Double amount;

    private LocalDateTime paidAt;
    @Column(nullable = false)
    private String status;
    private String receiptNumber;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonManagedReference // 👈 Prevents loop with Loan entity
    private Loan loan;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "installment_id")
    @JsonBackReference // 👈 Prevents loop with Installmentdetails entity
    private Installmentdetails installment;

    private LocalDateTime createdAt = LocalDateTime.now();
}