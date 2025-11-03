package com.example.microlending.service;

import com.example.microlending.entity.Installmentdetails;
import com.example.microlending.entity.Loan;
import com.example.microlending.entity.User;
import com.example.microlending.repository.InstallmentRepository;
import com.example.microlending.repository.LoanRepository;
import com.example.microlending.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import org.springframework.core.io.InputStreamResource;
import java.io.ByteArrayInputStream;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public LoanService(LoanRepository loanRepository,
                       InstallmentRepository installmentRepository,
                       PaymentRepository paymentRepository,
                       EmailService emailService,
                       S3Client s3Client) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
        this.s3Client = s3Client;
    }

    public List<Loan> findByUserId(Long userId) {
        return loanRepository.findByUser_Userid(userId);
    }

    public List<Loan> findAll() {
        return loanRepository.findAllWithUser(); // Uses the efficient query
    }

    public Loan findById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
    }

    // Helper method to upload a single file
    private String uploadFileToS3(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String fileName = "loan-documents/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return fileName;
    }

    // ✅ Step 1: User requests loan with TWO documents
    @Transactional
    public Loan requestLoan(User user, Double amount, String purpose, MultipartFile document, MultipartFile document2) {
        try {
            String s3Path1 = uploadFileToS3(document);
            String s3Path2 = uploadFileToS3(document2);

            Loan loan = Loan.builder()
                    .user(user)
                    .amount(amount)
                    .purpose(purpose)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .documentName(document.getOriginalFilename())
                    .documentType(document.getContentType())
                    .documentPath(s3Path1) // S3 key 1
                    .document2Name(document2.getOriginalFilename())
                    .document2Type(document2.getContentType())
                    .document2Path(s3Path2) // S3 key 2
                    .build();

            return loanRepository.save(loan);

        } catch (IOException e) {
            throw new RuntimeException("❌ Failed to upload document(s) to S3", e);
        }
    }

    // Helper method to get a file from S3
    private Resource getFileFromS3(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            throw new RuntimeException("Document path is null or empty.");
        }
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            byte[] content = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            return new InputStreamResource(new ByteArrayInputStream(content));

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to fetch document from S3", e);
        }
    }

    // ✅ Admin retrieves loan document 1
    public Resource getLoanDocument(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        return getFileFromS3(loan.getDocumentPath());
    }

    // ✅ Admin retrieves loan document 2
    public Resource getLoanDocument2(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        return getFileFromS3(loan.getDocument2Path());
    }


    // ✅ Step 2: Admin approves loan
    @Transactional
    public Loan approveLoan(Long loanId, Double adminProvidedRate, Integer overrideTermMonths) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (adminProvidedRate == null || adminProvidedRate < 0 || adminProvidedRate > 100) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 100");
        }

        if (!"PENDING".equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in PENDING state. Current status: " + loan.getStatus());
        }

        loan.setStatus("APPROVED");
        loan.setInterestRate(adminProvidedRate);

        if (overrideTermMonths != null && overrideTermMonths > 0) {
            loan.setTermMonths(overrideTermMonths);
        }

        if (loan.getTermMonths() == null || loan.getTermMonths() <= 0) {
            throw new IllegalStateException("Loan term is not set or is invalid for loan ID: " + loanId);
        }

        // Calculate interest, total amount
        BigDecimal principal = BigDecimal.valueOf(loan.getAmount());
        BigDecimal rate = BigDecimal.valueOf(adminProvidedRate);
        BigDecimal tenureYears = BigDecimal.valueOf(loan.getTermMonths())
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal interestAmount = principal
                .multiply(rate)
                .multiply(tenureYears)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        loan.setInterestAmount(interestAmount);
        loan.setTotalAmount(principal.add(interestAmount));

        Loan savedLoan = loanRepository.save(loan);

        generateInstallments(savedLoan.getLoanid());
        sendLoanApprovalEmail(savedLoan);

        return savedLoan;
    }

    // ✅ Step 2b: Admin rejects loan
    @Transactional
    public Loan rejectLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with id " + loanId));

        if (!"PENDING".equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in PENDING state. Current status: " + loan.getStatus());
        }

        loan.setStatus("REJECTED");
        Loan savedLoan = loanRepository.save(loan);

        sendLoanRejectionEmail(savedLoan, "Loan did not meet eligibility criteria");

        return savedLoan;
    }

    // ✅ Step 3: Generate installments (prevent duplicates)
    public List<Installmentdetails> generateInstallments(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        List<Installmentdetails> existing = installmentRepository.findByLoan_Loanid(loanId);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }

        if (!"APPROVED".equals(loan.getStatus())) {
            throw new IllegalStateException("Installments can only be generated for an APPROVED loan. Current status: " + loan.getStatus());
        }

        if (loan.getTermMonths() == null || loan.getTermMonths() <= 0) {
            throw new IllegalStateException("Loan term is not set or is invalid for loan ID: " + loanId);
        }

        BigDecimal monthlyInstallment = loan.getTotalAmount()
                .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);

        LocalDate startDate = loan.getCreatedAt().toLocalDate();

        List<Installmentdetails> installments = new ArrayList<>();
        BigDecimal totalCalculated = BigDecimal.ZERO;

        for (int i = 1; i <= loan.getTermMonths(); i++) {
            BigDecimal amountThisMonth = monthlyInstallment;

            if (i == loan.getTermMonths()) {
                amountThisMonth = loan.getTotalAmount().subtract(totalCalculated);
            }

            Installmentdetails installment = Installmentdetails.builder()
                    .monthNumber(i)
                    .dueDate(startDate.plusMonths(i))
                    .amount(amountThisMonth)
                    .paid(false)
                    .status("PENDING")
                    .loan(loan)
                    .build();
            installments.add(installment);
            totalCalculated = totalCalculated.add(amountThisMonth);
        }

        return installmentRepository.saveAll(installments);
    }

    // --- All Email Methods ---

    public void sendLoanApprovalEmail(Loan loan) {
        User user = loan.getUser();
        if (user == null || user.getEmail() == null) {
            System.err.println("Cannot send approval email: User or user email is null for loan ID: " + loan.getLoanid());
            return;
        }
        String subject = "Loan Approved - MICR-LOAN-" + loan.getLoanid();
        String body = "<p>Dear " + user.getName() + ",</p>" +
                "<p>Congratulations! Your loan (<b>MICR-LOAN-" + loan.getLoanid() + "</b>) has been approved.</p>" +
                "<ul>" +
                "<li><b>Loan Amount:</b> ₹" + String.format("%.2f", loan.getAmount()) + "</li>" +
                "<li><b>Total Repayable:</b> ₹" + String.format("%.2f", loan.getTotalAmount()) + "</li>" +
                "<li><b>Term:</b> " + loan.getTermMonths() + " months</li>" +
                "</ul>" +
                "<p>You can log in to your dashboard to view your installment schedule.</p>" +
                "<p>Best Regards,<br/>MicroLending Team</p>";

        emailService.sendHtmlEmail(user.getEmail(), subject, body);
    }

    public void sendLoanRejectionEmail(Loan loan, String reason) {
        User user = loan.getUser();
        if (user == null || user.getEmail() == null) {
            System.err.println("Cannot send rejection email: User or user email is null for loan ID: " + loan.getLoanid());
            return;
        }
        String subject = "Loan Rejected - MICR-LOAN-" + loan.getLoanid();
        String body = "<p>Dear " + user.getName() + ",</p>" +
                "<p>We regret to inform you that your loan request (<b>MICR-LOAN-" + loan.getLoanid() + "</b>) has been rejected.</p>" +
                "<p><b>Reason:</b> " + reason + "</p>" +
                "<p>Best Regards,<br/>MicroLending Team</p>";

        emailService.sendHtmlEmail(user.getEmail(), subject, body);
    }

    public void sendLoanCompletionEmail(Loan loan) {
        User user = loan.getUser();
        if (user == null || user.getEmail() == null) {
            System.err.println("Cannot send completion email: User or user email is null for loan ID: " + loan.getLoanid());
            return;
        }
        String subject = "Loan Closed - MICR-LOAN-" + loan.getLoanid();
        String body = "<p>Dear " + user.getName() + ",</p>" +
                "<p>Congratulations! You have successfully repaid all installments.</p>" +
                "<p>Your loan (<b>MICR-LOAN-" + loan.getLoanid() + "</b>) is now <b>CLOSED</b>.</p>" +
                "<p>Best Regards,<br/>MicroLending Team</p>";

        emailService.sendHtmlEmail(user.getEmail(), subject, body);
    }
}