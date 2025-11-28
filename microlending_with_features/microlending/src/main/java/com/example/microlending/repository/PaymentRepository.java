package com.example.microlending.repository;

import com.example.microlending.entity.Loan;
import com.example.microlending.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {


    @Query("SELECT p FROM Payment p JOIN FETCH p.loan l WHERE l.user.userid = :userId")
    List<Payment> findByUserIdWithDetails(@Param("userId") Long userId);


     //This query is for the ADMIN'S "All Payments" tab.

    @Query("SELECT p FROM Payment p JOIN FETCH p.loan l JOIN FETCH l.user u")
    List<Payment> findAllWithDetails();

    // Used by PaymentService to check if a loan is fully paid
    List<Payment> findByLoanAndStatus(Loan loan, String status);

}

