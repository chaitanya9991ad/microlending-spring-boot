package com.example.microlending.scheduler;

import com.example.microlending.entity.Installmentdetails;
import com.example.microlending.repository.InstallmentRepository;
import com.example.microlending.service.LoanService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReminderScheduler {

    private final LoanService loanService;
    private final InstallmentRepository installmentRepository;

    public ReminderScheduler(LoanService loanService,
                             InstallmentRepository installmentRepository) {
        this.loanService = loanService;
        this.installmentRepository = installmentRepository;
    }

    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendInstallmentReminders() {
        LocalDate today = LocalDate.now();

        // Fetch all installments due today or overdue and not paid
        List<Installmentdetails> dueInstallments = installmentRepository.findAll()
                .stream()
                .filter(i -> !i.isPaid() && !i.getDueDate().isAfter(today))
                .toList();

        if (dueInstallments.isEmpty()) {
            System.out.println("No installments due today.");
            return;
        }

        for (Installmentdetails installment : dueInstallments) {
            // Here you can integrate email/SMS service
            System.out.println("Reminder: Installment " + installment.getInstallmentid() +
                    " for Loan " + installment.getLoan().getLoanid() +
                    " is due on " + installment.getDueDate());
        }
    }
}
