package com.example.microlending.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Installmentdetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long installmentid;

    private Integer monthNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String status;
    private boolean paid;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonBackReference // Breaks the serialization loop with the Loan entity.
    private Loan loan;
}