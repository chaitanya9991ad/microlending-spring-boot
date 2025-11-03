package com.example.microlending.repository;

import com.example.microlending.entity.Loan;
import com.example.microlending.entity.User; // Ensure User is imported if used in other queries
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Fetch loans by the User's primary key field 'userid'
    // This is used by your DashboardController
    List<Loan> findByUser_Userid(Long userId);

    /**
     * THIS IS THE FIX:
     * This query fetches all Loan entities and simultaneously fetches (JOINS)
     * the associated User entity for each loan. This is what solves the
     * "N/A" problem and is called by your LoanService's findAll() method.
     */
    @Query("SELECT l FROM Loan l JOIN FETCH l.user")
    List<Loan> findAllWithUser();
}

