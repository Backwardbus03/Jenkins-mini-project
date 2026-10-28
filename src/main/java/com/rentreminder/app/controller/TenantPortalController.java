package com.rentreminder.app.controller;

import com.rentreminder.app.model.AppNotification;
import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.repository.PaymentRepository;
import com.rentreminder.app.service.NotificationService;
import com.rentreminder.app.service.OwnerAccountService;
import com.rentreminder.app.service.PaymentService;
import com.rentreminder.app.service.QrCodeService;
import com.rentreminder.app.service.TenantAccountService;
import com.rentreminder.app.service.TenantInvitationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles all /portal/** routes for the self-service Tenant Portal.
 */
@Controller
@RequestMapping("/portal")
public class TenantPortalController {

    private final TenantAccountService accountService;
    private final TenantInvitationService invitationService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final QrCodeService qrCodeService;
    private final OwnerAccountService ownerAccountService;
    private final NotificationService notificationService;

    public TenantPortalController(TenantAccountService accountService,
                                  TenantInvitationService invitationService,
                                  PaymentRepository paymentRepository,
                                  PaymentService paymentService,
                                  QrCodeService qrCodeService,
                                  OwnerAccountService ownerAccountService,
                                  NotificationService notificationService) {
        this.accountService = accountService;
        this.invitationService = invitationService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.qrCodeService = qrCodeService;
        this.ownerAccountService = ownerAccountService;
        this.notificationService = notificationService;
    }

    @GetMapping({"", "/"})
    public String portalHome() {
        return "redirect:/portal/dashboard";
    }

    // ── Registration ──────────────────────────────────────────────────────

    @GetMapping({ "/register", "/signup" })
    public String showRegisterForm() {
        return "portal/register";
    }

    @PostMapping({ "/register", "/signup" })
    public String register(@RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/portal/register";
        }
        try {
            accountService.register(name, email.toLowerCase().trim(), phone, password);
            redirectAttributes.addFlashAttribute("success",
                    "Account created! Please log in to view your invitations.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/portal/register";
        }
    }

    // ── Dashboard ─────────────────────────────────────────────────────────

    @GetMapping({ "/dashboard", "/home" })
    public String dashboard(Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null)
            return "redirect:/login";

        List<TenantInvitation> pending = invitationService.getPendingForAccount(account);
        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);

        List<Payment> allPayments = new ArrayList<>();
        List<Payment> overduePayments = new ArrayList<>();
        List<Payment> unpaidPayments = new ArrayList<>();
        List<String> urgentReminders = new ArrayList<>();
        List<AppNotification> tenantNotifications = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // For each approved lease, fetch payment history and notifications
        approved.forEach(invite -> {
            var tenant = invite.getTenant();
            var tenantPayments = paymentRepository.findByTenant(tenant);
            allPayments.addAll(tenantPayments);

            var tenantOverdue = tenantPayments.stream()
                    .filter(p -> "Overdue".equalsIgnoreCase(p.getStatus()))
                    .toList();
            overduePayments.addAll(tenantOverdue);
            model.addAttribute("overduePayments_" + invite.getId(), tenantOverdue);

            // Collect active bills needing payment or confirmation
            tenantPayments.stream()
                    .filter(p -> "Pending".equalsIgnoreCase(p.getStatus())
                            || "Overdue".equalsIgnoreCase(p.getStatus())
                            || "Awaiting Confirmation".equalsIgnoreCase(p.getStatus()))
                    .forEach(p -> {
                        unpaidPayments.add(p);
                        if (p.getDueDate() != null && !"Paid".equalsIgnoreCase(p.getStatus())) {
                            long days = java.time.temporal.ChronoUnit.DAYS.between(today, p.getDueDate());
                            String billLabel = p.isElectricity() ? "Electricity bill" : "Rent";
                            if (days < 0) {
                                urgentReminders.add(String.format(
                                        "⚠️ %s for Apt %s is OVERDUE by %d day(s) (Due: %s, Amount: ₹%.2f)",
                                        billLabel, invite.getTenant().getApartmentNumber(), Math.abs(days),
                                        p.getDueDate(), p.getAmount()));
                            } else if (days == 0) {
                                urgentReminders.add(String.format("🚨 %s for Apt %s is due TODAY (Amount: ₹%.2f)",
                                        billLabel, invite.getTenant().getApartmentNumber(), p.getAmount()));
                            } else if (days <= 3) {
                                urgentReminders
                                        .add(String.format("⏰ %s for Apt %s is due in %d day(s) on %s (Amount: ₹%.2f)",
                                                billLabel, invite.getTenant().getApartmentNumber(), days,
                                                p.getDueDate(), p.getAmount()));
                            }
                        }
                    });

            // Fetch stored in-app notifications
            tenantNotifications.addAll(notificationService.getNotificationsForTenant(tenant));
        });

        long unreadCount = tenantNotifications.stream().filter(n -> !n.isReadStatus()).count();

        double totalDue = unpaidPayments.stream()
                .filter(p -> !"Awaiting Confirmation".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        model.addAttribute("account", account);
        model.addAttribute("pending", pending);
        model.addAttribute("approved", approved);
        model.addAttribute("allPayments", allPayments);
        model.addAttribute("overduePayments", overduePayments);
        model.addAttribute("unpaidPayments", unpaidPayments);
        model.addAttribute("totalDue", totalDue);
        model.addAttribute("urgentReminders", urgentReminders);
        model.addAttribute("notifications", tenantNotifications);
        model.addAttribute("unreadNotificationCount", unreadCount);
        return "portal/dashboard";
    }

    // ── Notifications ─────────────────────────────────────────────────────

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/portal/dashboard";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsRead(Authentication auth) {
        TenantAccount account = resolveAccount(auth);
        if (account != null) {
            List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);
            for (TenantInvitation inv : approved) {
                if (inv.getTenant() != null) {
                    notificationService.markAllAsReadForTenant(inv.getTenant());
                }
            }
        }
        return "redirect:/portal/dashboard";
    }

    // ── QR Code & Payment Confirmation ───────────────────────────────────

    @GetMapping("/payments/{id}/qr")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> getPaymentQrCode(@PathVariable Long id,
            Authentication auth) {
        TenantAccount account = resolveAccount(auth);
        if (account == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .build();
        }

        Optional<Payment> opt = paymentRepository.findById(id);
        if (opt.isEmpty()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        Payment payment = opt.get();

        // Security check: ensure payment belongs to one of this tenant's approved
        // leases
        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);
        boolean belongsToTenant = approved.stream()
                .anyMatch(inv -> inv.getTenant().getId().equals(payment.getTenant().getId()));
        if (!belongsToTenant) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .build();
        }

        String ownerUpi = ownerAccountService.getDefaultOwnerUpi();
        String ownerName = ownerAccountService.getDefaultOwnerName();
        String note = String.format("%s-%d-%d-Apt%s",
                payment.getType(), payment.getMonth(), payment.getYear(), payment.getTenant().getApartmentNumber());

        String upiUri = qrCodeService.buildUpiUri(ownerUpi, ownerName, payment.getAmount(), note);
        byte[] qrBytes = qrCodeService.generateQrPng(upiUri, 220, 220);

        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                .body(qrBytes);
    }

    @PostMapping("/payments/{id}/mark-paid")
    public String markPaid(@PathVariable Long id,
            @RequestParam String upiReference,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        TenantAccount account = resolveAccount(auth);
        if (account == null)
            return "redirect:/login";

        Optional<Payment> opt = paymentRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Payment record not found.");
            return "redirect:/portal/dashboard";
        }
        Payment payment = opt.get();

        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);
        boolean belongsToTenant = approved.stream()
                .anyMatch(inv -> inv.getTenant().getId().equals(payment.getTenant().getId()));
        if (!belongsToTenant) {
            redirectAttributes.addFlashAttribute("error", "Unauthorized access to payment record.");
            return "redirect:/portal/dashboard";
        }

        try {
            paymentService.markPaymentPaidByTenant(id, upiReference.trim());

            // Create notification for the Owner so they see the submitted payment
            String billLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
            String tenantName = payment.getTenant().getName();
            String apt = payment.getTenant().getApartmentNumber();
            notificationService.notifyOwner(
                    String.format("💳 Payment Submitted: Apt %s (%s)", apt, billLabel),
                    String.format("%s submitted UPI Ref %s for %s (₹%.2f). Verification required.",
                            tenantName, upiReference.trim(), billLabel,
                            payment.getAmount() != null ? payment.getAmount() : 0.0),
                    "INFO",
                    "/payments/summary",
                    payment);

            redirectAttributes.addFlashAttribute("success",
                    "Payment marked as submitted! The owner will verify the UPI reference (" + upiReference.trim()
                            + ") and update the receipt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/portal/dashboard";
    }

    // ── Invitation actions ────────────────────────────────────────────────

    @PostMapping("/invite/{id}/approve")
    public String approveInvite(@PathVariable Long id,
            @RequestParam Double depositAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalEndDate,
            RedirectAttributes redirectAttributes) {
        try {
            invitationService.approve(id, depositAmount, rentalStartDate, rentalEndDate);
            redirectAttributes.addFlashAttribute("success", "Invitation approved! Your rental is now active.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/portal/dashboard";
    }

    @PostMapping("/invite/{id}/reject")
    public String rejectInvite(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invitationService.reject(id);
            redirectAttributes.addFlashAttribute("success", "Invitation rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/portal/dashboard";
    }

    // ── Receipts for Tenants ──────────────────────────────────────────────

    @GetMapping({ "/receipt/{id}", "/receipts/{id}" })
    public String viewReceipt(@PathVariable Long id, Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null)
            return "redirect:/login";

        Optional<Payment> opt = paymentRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/portal/dashboard";
        }
        Payment payment = opt.get();

        // Verify the payment belongs to this tenant
        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);
        boolean belongsToTenant = approved.stream()
                .anyMatch(inv -> inv.getTenant().getId().equals(payment.getTenant().getId()));
        if (!belongsToTenant) {
            return "redirect:/portal/dashboard";
        }

        model.addAttribute("payments", List.of(payment));
        model.addAttribute("singleReceipt", true);
        model.addAttribute("isTenantPortal", true);
        return "payments/receipt";
    }

    @GetMapping({ "/receipt", "/receipts" })
    public String viewAllReceipts(Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null)
            return "redirect:/login";

        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);
        List<Payment> allPayments = approved.stream()
                .flatMap(inv -> paymentRepository.findByTenant(inv.getTenant()).stream())
                .toList();

        model.addAttribute("payments", allPayments);
        model.addAttribute("singleReceipt", false);
        model.addAttribute("isTenantPortal", true);
        return "payments/receipt";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private TenantAccount resolveAccount(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return null;
        String email = auth.getName();
        Optional<TenantAccount> opt = accountService.findByEmail(email);
        return opt.orElse(null);
    }
}
