package com.example.microlending.service;

import com.example.microlending.entity.Installmentdetails;
import com.example.microlending.entity.Loan;
import com.example.microlending.entity.Payment;
import com.example.microlending.entity.User;
import com.example.microlending.repository.InstallmentRepository;
import com.example.microlending.repository.LoanRepository;
import com.example.microlending.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InstallmentRepository installmentRepository;
    private final LoanRepository loanRepository;
    private final LoanService loanService;
    private final EmailService emailService;

    public PaymentService(PaymentRepository paymentRepository,
                          InstallmentRepository installmentRepository,
                          LoanRepository loanRepository,
                          LoanService loanService,
                          EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.installmentRepository = installmentRepository;
        this.loanRepository = loanRepository;
        this.loanService = loanService;
        this.emailService = emailService;
    }

    // For User "My Payments" tab
    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findByUserIdWithDetails(userId);
    }

    // For Admin "All Payments" tab
    public List<Payment> findAllWithDetails() {
        return paymentRepository.findAllWithDetails();
    }

    @Transactional
    public Payment initiateDummyPayment(Long loanId, Long installmentId, double amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        Payment payment = Payment.builder()
                .loan(loan)
                .amount(amount)
                .status("PENDING")
                .transactionId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        if (installmentId != null) {
            Installmentdetails installment = installmentRepository.findById(installmentId)
                    .orElseThrow(() -> new RuntimeException("Installment not found"));
            payment.setInstallment(installment);
        }

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment verifyDummyPayment(Long paymentId, boolean success) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Set the status based on the 'success' parameter
        payment.setStatus(success ? "SUCCESS" : "FAILED");
        if (success) {
            payment.setPaidAt(LocalDateTime.now());
        }
        // Save the payment *after* updating its status
        Payment savedPayment = paymentRepository.save(payment);

        if (success && savedPayment.getInstallment() != null) {
            Installmentdetails installment = savedPayment.getInstallment();
            installment.setPaid(true);
            installment.setStatus("PAID");
            installmentRepository.save(installment);

            Loan loan = installment.getLoan();

            List<Installmentdetails> allInstallments = installmentRepository.findByLoan_Loanid(loan.getLoanid());
            boolean allPaid = allInstallments.stream().allMatch(Installmentdetails::isPaid);

            if (allPaid) {
                loan.setStatus("CLOSED");
                loanRepository.save(loan);
                loanService.sendLoanCompletionEmail(loan);
            }
        }

        // Pass the *saved* payment (with updated status) to the email service
        sendPaymentReceiptEmail(savedPayment);
        return savedPayment;
    }

    private void sendPaymentReceiptEmail(Payment payment) {
        // Safe-guard against missing data during serialization
        if (payment.getLoan() == null || payment.getLoan().getUser() == null) {
            System.err.println("Cannot send payment receipt: Loan or User data is missing for Payment ID: " + payment.getPaymentid());
            return;
        }

        User user = payment.getLoan().getUser();
        if (user.getEmail() == null || user.getEmail().isEmpty()) return;

        Loan loan = payment.getLoan();

        List<Payment> successfulPayments = paymentRepository.findByLoanAndStatus(loan, "SUCCESS");
        double totalPaid = successfulPayments.stream().mapToDouble(Payment::getAmount).sum();


        if ("SUCCESS".equals(payment.getStatus())) {

            boolean alreadyCounted = successfulPayments.stream()
                    .anyMatch(p -> p.getPaymentid().equals(payment.getPaymentid()));
            if (!alreadyCounted) {
                totalPaid += payment.getAmount();
            }
        }


        double remainingAmount = (loan.getTotalAmount() != null ? loan.getTotalAmount().doubleValue() : 0.0) - totalPaid;

        String subject = "Loan Payment Receipt - MICR-LOAN-" + loan.getLoanid();
        String body = "<p>Dear " + user.getName() + ",</p>" +
                "<p>Thank you for your payment. Here are the details:</p>" +
                "<ul>" +
                "<li><b>Payment ID:</b> MICR-PAY-" + payment.getPaymentid() + "</li>" +
                "<li><b>Loan ID:</b> MICR-LOAN-" + loan.getLoanid() + "</li>" +
                "<li><b>Amount Paid:</b> ₹" + String.format("%.2f", payment.getAmount()) + "</li>" +
                "<li><b>Status:</b> " + payment.getStatus() + "</li>" +
                "<li><b>Transaction ID:</b> " + payment.getTransactionId() + "</li>" +
                "<li><b>Date:</b> " + (payment.getPaidAt() != null ? payment.getPaidAt().toString() : "N/A") + "</li>" +
                (payment.getInstallment() != null ? "<li><b>Installment #" + payment.getInstallment().getMonthNumber() + "</b> marked as PAID</li>" : "") +
                "</ul>" +
                "<p><b>Remaining Loan Amount: ₹" + String.format("%.2f", remainingAmount) + "</b></p>" +
                "<p>Best Regards,<br/>MicroLending Team</p>";

        emailService.sendHtmlEmail(user.getEmail(), subject, body);
    }
}

