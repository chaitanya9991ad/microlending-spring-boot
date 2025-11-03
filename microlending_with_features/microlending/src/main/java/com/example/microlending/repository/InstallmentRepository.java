package com.example.microlending.repository;

import com.example.microlending.entity.Installmentdetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installmentdetails, Long> {
    List<Installmentdetails> findByLoan_Loanid(Long loanid);
}



