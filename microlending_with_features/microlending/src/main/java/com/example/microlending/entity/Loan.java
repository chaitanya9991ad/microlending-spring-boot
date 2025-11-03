package com.example.microlending.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanid;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String purpose;

    // Loan approval details
    private Double interestRate;
    private BigDecimal interestAmount;
    private BigDecimal totalAmount;
    private Integer termMonths;

    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime createdAt = LocalDateTime.now();

    // Document 1 details
    private String documentName;
    private String documentType;
    private String documentPath; // S3 Key 1

    // Document 2 details
    private String document2Name;
    private String document2Type;
    private String document2Path; // S3 Key 2


    // --- RELATIONSHIPS (Corrected) ---

    /**
     * Connects to the User.
     * FIX: Swapped to @JsonManagedReference.
     * This tells the serializer to INCLUDE the 'user' object.
     * This will fix the "(User N/A)" in your "Loan Applications" table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonManagedReference // 👈 THIS IS THE FIX
    private User user;

    /**
     * Connects to the Installments.
     * FIX: Swapped to @JsonBackReference.
     * This HIDES the list and breaks the infinite loop.
     */
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference // 👈 THIS IS THE FIX
    private List<Installmentdetails> installments;

    /**
     * Connects to the Payments.
     * FIX: Swapped to @JsonBackReference.
     * This HIDES the list and breaks the (Payment -> Loan -> List<Payments>) loop
     * started by your Payment.java file.
     */
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference // 👈 THIS IS THE FIX
    private List<Payment> payments;
}

