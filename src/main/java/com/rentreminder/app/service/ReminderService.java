package com.rentreminder.app.service;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final PaymentRepository paymentRepository;
    private final TenantInvitationService tenantInvitationService;
    private final JavaMailSender mailSender;

    @Value("${app.reminder.enabled:false}")
    private boolean remindersEnabled;

    @Value("${app.portal.base-url:http://localhost:8081}")
    private String portalBaseUrl;

    @Value("${spring.mail.username:noreply@rentportal.com}")
    private String fromEmail;

    public ReminderService(PaymentRepository paymentRepository,
                           TenantInvitationService tenantInvitationService,
                           JavaMailSender mailSender) {
        this.paymentRepository = paymentRepository;
        this.tenantInvitationService = tenantInvitationService;
        this.mailSender = mailSender;
    }

    /**
     * Daily scheduled task to scan for due/overdue rent and electricity bills
     * and dispatch reminder emails.
     */
    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * *}")
    public int processDueReminders() {
        if (!remindersEnabled) {
            log.debug("Automated reminders are disabled via app.reminder.enabled=false");
            return 0;
        }

        LocalDate today = LocalDate.now();
        List<Payment> candidatePayments = paymentRepository.findAll().stream()
                .filter(p -> "Pending".equalsIgnoreCase(p.getStatus()) || "Overdue".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getDueDate() != null)
                .toList();

        int sentCount = 0;
        for (Payment payment : candidatePayments) {
            LocalDate dueDate = payment.getDueDate();
            long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

            // Trigger when due within 3 days, due today, or already overdue
            boolean isEligible = daysUntilDue <= 3;
            boolean alreadyRemindedToday = today.equals(payment.getLastReminderSentOn());

            if (isEligible && !alreadyRemindedToday) {
                // If past due and still 'Pending', transition to 'Overdue'
                if (daysUntilDue < 0 && "Pending".equalsIgnoreCase(payment.getStatus())) {
                    payment.setStatus("Overdue");
                    if (payment.isRent() && payment.getAmount() != null) {
                        tenantInvitationService.deductFromDeposit(payment.getTenant(), payment.getAmount());
                    }
                }

                boolean success = sendEmailReminder(payment, daysUntilDue);
                if (success) {
                    payment.setLastReminderSentOn(today);
                    paymentRepository.save(payment);
                    sentCount++;
                }
            }
        }
        log.info("ReminderService dispatched {} reminder emails for today", sentCount);
        return sentCount;
    }

    /**
     * Trigger a single reminder on-demand (e.g. for viva demonstration or manual landlord trigger).
     */
    public boolean sendManualReminder(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        LocalDate today = LocalDate.now();
        LocalDate dueDate = payment.getDueDate() != null ? payment.getDueDate() : today;
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        boolean success = sendEmailReminder(payment, daysUntilDue);
        if (success) {
            payment.setLastReminderSentOn(today);
            paymentRepository.save(payment);
        }
        return success;
    }

    private boolean sendEmailReminder(Payment payment, long daysUntilDue) {
        if (payment.getTenant() == null || payment.getTenant().getEmail() == null) {
            log.warn("Skipping reminder for payment #{}: no tenant email found", payment.getId());
            return false;
        }

        String billTypeLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
        String recipient = payment.getTenant().getEmail();
        String subject;

        if (daysUntilDue > 0) {
            subject = String.format("⏰ Reminder: %s Payment Due in %d Day(s)", billTypeLabel, daysUntilDue);
        } else if (daysUntilDue == 0) {
            subject = String.format("🚨 Due Today: %s Payment for Apt %s", billTypeLabel, payment.getTenant().getApartmentNumber());
        } else {
            subject = String.format("⚠️ OVERDUE: %s Payment for Apt %s", billTypeLabel, payment.getTenant().getApartmentNumber());
        }

        String body = String.format(
                "Dear %s,\n\n" +
                "This is a reminder from Rent Reminder Portal regarding your %s for Apartment %s.\n\n" +
                "• Billing Period: Month %d / %d\n" +
                "• Amount Due: ₹%.2f\n" +
                "• Due Date: %s\n" +
                "• Status: %s\n\n" +
                "You can scan the UPI QR code and submit your payment directly on the Tenant Portal:\n" +
                "%s/portal/dashboard\n\n" +
                "Thank you,\n" +
                "Property Management",
                payment.getTenant().getName(),
                billTypeLabel,
                payment.getTenant().getApartmentNumber(),
                payment.getMonth(),
                payment.getYear(),
                payment.getAmount() != null ? payment.getAmount() : 0.0,
                payment.getDueDate() != null ? payment.getDueDate().toString() : "Immediately",
                payment.getStatus(),
                portalBaseUrl
        );

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent {} reminder email to {}", billTypeLabel, recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to send reminder email to {}: {}", recipient, e.getMessage());
            // In demo / staging environments where SMTP might not be live, log gracefully
            return false;
        }
    }
}
