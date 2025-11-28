package com.example.microlending.controller;

import com.example.microlending.entity.Loan;
import com.example.microlending.service.LoanService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/loans")
public class AdminController {

    private final LoanService loanService;

    public AdminController(LoanService loanService) {
        this.loanService = loanService;
    }

    //  Approve Loan (Matches index.html)
    @PutMapping("/approve/{loanId}")
    public ResponseEntity<Loan> approveLoan(@PathVariable Long loanId,
                                            @RequestParam Double interestRate,
                                            @RequestParam int termMonths) {
        Loan loan = loanService.approveLoan(loanId, interestRate, termMonths);
        return ResponseEntity.ok(loan);
    }

    //  Reject Loan
    @PutMapping("/reject/{loanId}")
    public ResponseEntity<String> rejectLoan(@PathVariable Long loanId) {
        loanService.rejectLoan(loanId);
        return ResponseEntity.ok("Loan rejected successfully");
    }

    //  View all loans (Uses query with JOIN FETCH User)
    @GetMapping("/getloans")
    public ResponseEntity<?> viewAllLoans() {
        return ResponseEntity.ok(loanService.findAll());
    }

    //  Download or Preview Loan Document 1
    @GetMapping("/{loanId}/document")
    public ResponseEntity<Resource> getLoanDocument(@PathVariable Long loanId,
                                                    @RequestParam(defaultValue = "download") String mode) {
        try {
            Resource resource = loanService.getLoanDocument(loanId);
            Loan loan = loanService.findById(loanId);

            String disposition = mode.equalsIgnoreCase("preview") ? "inline" : "attachment";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + loan.getDocumentName() + "\"")
                    .contentType(MediaType.parseMediaType(loan.getDocumentType()))
                    .body(resource);
        } catch (Exception e) {
            // Log the error e.getMessage()
            return ResponseEntity.status(404).build();
        }
    }

    //  Download or Preview Loan Document 2
    @GetMapping("/{loanId}/document2")
    public ResponseEntity<Resource> getLoanDocument2(@PathVariable Long loanId,
                                                     @RequestParam(defaultValue = "download") String mode) {
        try {
            Resource resource = loanService.getLoanDocument2(loanId);
            Loan loan = loanService.findById(loanId);

            String disposition = mode.equalsIgnoreCase("preview") ? "inline" : "attachment";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + loan.getDocument2Name() + "\"")
                    .contentType(MediaType.parseMediaType(loan.getDocument2Type()))
                    .body(resource);
        } catch (Exception e) {
            // Log the error e.getMessage()
            return ResponseEntity.status(404).build();
        }
    }
}