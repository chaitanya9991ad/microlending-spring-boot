package com.example.microlending.repository;

import com.example.microlending.entity.Loan;
import com.example.microlending.entity.User; // Ensure User is imported if used in other queries
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {


    List<Loan> findByUser_Userid(Long userId);


     //This query fetches all Loan entities and simultaneously fetches (JOINS)

    @Query("SELECT l FROM Loan l JOIN FETCH l.user")
    List<Loan> findAllWithUser();
}

