package com.rentreminder.app.service;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final NotificationService notificationService;
    private final JavaMailSender mailSender;

    @Value("${app.reminder.enabled:true}")
    private boolean remindersEnabled;

    @Value("${app.portal.base-url:http://localhost:8081}")
    private String portalBaseUrl;

    @Value("${spring.mail.username:noreply@rentportal.com}")
    private String fromEmail;

    public ReminderService(PaymentRepository paymentRepository,
                           TenantInvitationService tenantInvitationService,
                           NotificationService notificationService,
                           @Autowired(required = false) JavaMailSender mailSender) {
        this.paymentRepository = paymentRepository;
        this.tenantInvitationService = tenantInvitationService;
        this.notificationService = notificationService;
        this.mailSender = mailSender;
    }

    /**
     * Daily scheduled task to scan for due/overdue rent and electricity bills
     * and dispatch in-app UI notifications (and optional email if SMTP is configured).
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

                // 1. Deliver primary in-app UI notification
                dispatchUiNotification(payment, daysUntilDue);

                // 2. Deliver secondary email reminder if SMTP is available (swallowing errors gracefully)
                sendEmailReminderQuietly(payment, daysUntilDue);

                payment.setLastReminderSentOn(today);
                paymentRepository.save(payment);
                sentCount++;
            }
        }
        log.info("ReminderService dispatched {} in-app UI reminders for today", sentCount);
        return sentCount;
    }

    /**
     * Trigger a single reminder on-demand (e.g. when landlord clicks 'Remind' in the UI).
     * Always creates an in-app UI notification for the tenant's portal dashboard.
     */
    public boolean sendManualReminder(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found with ID: " + paymentId));

        LocalDate today = LocalDate.now();
        LocalDate dueDate = payment.getDueDate() != null ? payment.getDueDate() : today;
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        // 1. Deliver primary in-app UI notification
        dispatchUiNotification(payment, daysUntilDue);

        // 2. Attempt email quietly without blocking or failing the operation
        sendEmailReminderQuietly(payment, daysUntilDue);

        payment.setLastReminderSentOn(today);
        paymentRepository.save(payment);

        return true;
    }

    /**
     * Creates an in-app UI notification visible to the tenant on their dashboard.
     */
    private void dispatchUiNotification(Payment payment, long daysUntilDue) {
        if (payment.getTenant() == null) {
            log.warn("Cannot create UI reminder for payment #{}: no tenant assigned", payment.getId());
            return;
        }

        String billTypeLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
        String apt = payment.getTenant().getApartmentNumber() != null ? payment.getTenant().getApartmentNumber() : "-";
        double amount = payment.getAmount() != null ? payment.getAmount() : 0.0;
        String dueDateStr = payment.getDueDate() != null ? payment.getDueDate().toString() : "Immediately";

        String title;
        String badgeType;
        if (daysUntilDue > 0) {
            title = String.format("⏰ %s Reminder: ₹%.2f due in %d day(s)", billTypeLabel, amount, daysUntilDue);
            badgeType = "WARNING";
        } else if (daysUntilDue == 0) {
            title = String.format("🚨 %s Due Today: ₹%.2f for Apt %s", billTypeLabel, amount, apt);
            badgeType = "DANGER";
        } else {
            title = String.format("⚠️ OVERDUE: %s of ₹%.2f for Apt %s", billTypeLabel, amount, apt);
            badgeType = "DANGER";
        }

        String message = String.format(
                "Your %s for Apt %s (Period %d/%d) of ₹%.2f is %s (Due: %s). " +
                "You can scan the UPI QR code directly on your dashboard to pay.",
                billTypeLabel,
                apt,
                payment.getMonth(),
                payment.getYear(),
                amount,
                daysUntilDue < 0 ? "OVERDUE" : (daysUntilDue == 0 ? "DUE TODAY" : "due soon"),
                dueDateStr
        );

        notificationService.notifyTenant(
                payment.getTenant(),
                title,
                message,
                badgeType,
                "/portal/dashboard#payments",
                payment
        );

        // If overdue, also notify the owner
        if (daysUntilDue < 0) {
            notificationService.notifyOwner(
                    String.format("⚠️ Overdue Alert: %s (Apt %s) - ₹%.2f", payment.getTenant().getName(), apt, amount),
                    String.format("%s for Apartment %s is overdue by %d days. Amount: ₹%.2f.", billTypeLabel, apt, Math.abs(daysUntilDue), amount),
                    "DANGER",
                    "/payments/summary",
                    payment
            );
        }
    }

    private void sendEmailReminderQuietly(Payment payment, long daysUntilDue) {
        if (mailSender == null || payment.getTenant() == null || payment.getTenant().getEmail() == null) {
            return;
        }

        try {
            String billTypeLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
            String recipient = payment.getTenant().getEmail();
            String subject = daysUntilDue > 0
                    ? String.format("⏰ Reminder: %s Payment Due in %d Day(s)", billTypeLabel, daysUntilDue)
                    : (daysUntilDue == 0
                    ? String.format("🚨 Due Today: %s Payment for Apt %s", billTypeLabel, payment.getTenant().getApartmentNumber())
                    : String.format("⚠️ OVERDUE: %s Payment for Apt %s", billTypeLabel, payment.getTenant().getApartmentNumber()));

            String body = String.format(
                    "Dear %s,\n\n" +
                    "This is a reminder regarding your %s for Apartment %s.\n" +
                    "• Amount Due: ₹%.2f\n" +
                    "• Due Date: %s\n" +
                    "• Status: %s\n\n" +
                    "View and pay via UPI QR: %s/portal/dashboard\n",
                    payment.getTenant().getName(),
                    billTypeLabel,
                    payment.getTenant().getApartmentNumber(),
                    payment.getAmount() != null ? payment.getAmount() : 0.0,
                    payment.getDueDate() != null ? payment.getDueDate().toString() : "Immediately",
                    payment.getStatus(),
                    portalBaseUrl
            );

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(recipient);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent optional reminder email to {}", recipient);
        } catch (Exception e) {
            log.debug("Email reminder delivery skipped/failed: {}", e.getMessage());
        }
    }
}
