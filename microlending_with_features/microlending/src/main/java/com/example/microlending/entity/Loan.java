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



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonManagedReference
    private User user;


    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Installmentdetails> installments;


    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    private List<Payment> payments;
}

