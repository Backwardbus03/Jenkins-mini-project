package com.rentreminder.app.controller;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.service.PaymentService;
import com.rentreminder.app.service.TenantInvitationService;
import com.rentreminder.app.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TenantService tenantService;
    private final TenantInvitationService tenantInvitationService;
    private final com.rentreminder.app.service.ReminderService reminderService;
    private final com.rentreminder.app.service.NotificationService notificationService;

    public PaymentController(PaymentService paymentService,
                             TenantService tenantService,
                             TenantInvitationService tenantInvitationService,
                             com.rentreminder.app.service.ReminderService reminderService,
                             com.rentreminder.app.service.NotificationService notificationService) {
        this.paymentService = paymentService;
        this.tenantService = tenantService;
        this.tenantInvitationService = tenantInvitationService;
        this.reminderService = reminderService;
        this.notificationService = notificationService;
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/payments/summary";
    }

    @GetMapping({"/summary", "/list"})
    public String showSummary(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        model.addAttribute("payments", payments);

        double totalCollected = payments.stream()
                .filter(p -> "Paid".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
        double totalPending = payments.stream()
                .filter(p -> "Pending".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
        double totalOverdue = payments.stream()
                .filter(p -> "Overdue".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
        long awaitingCount = payments.stream()
                .filter(p -> "Awaiting Confirmation".equalsIgnoreCase(p.getStatus()))
                .count();

        model.addAttribute("totalCollected", totalCollected);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalOverdue", totalOverdue);
        model.addAttribute("awaitingCount", awaitingCount);
        return "payments/summary";
    }

    @GetMapping({"/new", "/form", "/add"})
    public String showPaymentForm(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                  @RequestParam(value = "type", required = false) String type,
                                  Model model) {
        Payment payment = new Payment();
        payment.setType((type != null && !type.isBlank()) ? type.toUpperCase() : "RENT");
        payment.setStatus("Pending");
        payment.setDate(java.time.LocalDate.now());
        payment.setDueDate(java.time.LocalDate.now().plusDays(7));
        payment.setMonth(java.time.LocalDate.now().getMonthValue());
        payment.setYear(java.time.LocalDate.now().getYear());

        if (tenantId != null) {
            tenantService.findById(tenantId).ifPresent(payment::setTenant);
        }
        model.addAttribute("payment", payment);
        model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
        return "payments/form";
    }

    @PostMapping({"", "/"})
    public String savePayment(@Valid @ModelAttribute("payment") Payment payment, BindingResult bindingResult, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
            return "payments/form";
        }
        try {
            paymentService.savePayment(payment);
            redirectAttributes.addFlashAttribute("success", "Payment record saved successfully.");
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("month", "error.payment", e.getMessage());
            model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
            return "payments/form";
        }
        return "redirect:/payments/summary";
    }

    @PostMapping("/{id}/confirm")
    public String confirmPayment(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.confirmPayment(id);
            if (payment != null && payment.getTenant() != null) {
                String billLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
                notificationService.notifyTenant(
                        payment.getTenant(),
                        "✓ Payment Confirmed: " + billLabel,
                        String.format("Your %s payment of ₹%.2f (Ref: %s) has been verified and confirmed by the owner.",
                                billLabel, payment.getAmount() != null ? payment.getAmount() : 0.0,
                                payment.getUpiReference() != null ? payment.getUpiReference() : "-"),
                        "SUCCESS",
                        "/portal/dashboard#payments",
                        payment
                );
            }
            redirectAttributes.addFlashAttribute("success", "✓ Payment confirmed and verified. Notification sent to tenant.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments/summary";
    }

    @PostMapping("/{id}/reject")
    public String rejectPayment(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.rejectPayment(id);
            if (payment != null && payment.getTenant() != null) {
                String billLabel = payment.isElectricity() ? "Electricity Bill" : "Rent";
                notificationService.notifyTenant(
                        payment.getTenant(),
                        "⚠️ Payment Verification Failed: " + billLabel,
                        String.format("The owner was unable to verify UPI transaction '%s' for %s (₹%.2f). Please check your transaction details and re-submit.",
                                payment.getUpiReference() != null ? payment.getUpiReference() : "N/A",
                                billLabel, payment.getAmount() != null ? payment.getAmount() : 0.0),
                        "DANGER",
                        "/portal/dashboard#payments",
                        payment
                );
            }
            redirectAttributes.addFlashAttribute("error", "Payment reference rejected. Returned to Pending status.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments/summary";
    }

    @PostMapping("/{id}/remind")
    public String sendReminder(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            boolean sent = reminderService.sendManualReminder(id);
            var optPayment = paymentService.findById(id);
            String tenantName = optPayment.map(p -> p.getTenant() != null ? p.getTenant().getName() : "tenant").orElse("tenant");
            String apt = optPayment.map(p -> p.getTenant() != null ? p.getTenant().getApartmentNumber() : "").orElse("");
            redirectAttributes.addFlashAttribute("success", "✓ In-app UI reminder successfully delivered to " + tenantName + (apt.isEmpty() ? "" : " (Apt " + apt + ")") + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments/summary";
    }

    @GetMapping({"/receipt", "/receipts"})
    public String downloadReceipt(@RequestParam(value = "id", required = false) Long id, Model model) {
        if (id != null) {
            paymentService.findById(id).ifPresentOrElse(
                p -> {
                    model.addAttribute("payments", List.of(p));
                    model.addAttribute("singleReceipt", true);
                },
                () -> {
                    model.addAttribute("payments", paymentService.getAllPayments());
                    model.addAttribute("singleReceipt", false);
                }
            );
        } else {
            model.addAttribute("payments", paymentService.getAllPayments());
            model.addAttribute("singleReceipt", false);
        }
        model.addAttribute("isTenantPortal", false);
        return "payments/receipt";
    }

    @GetMapping({"/receipt/{id}", "/receipts/{id}"})
    public String viewReceiptById(@PathVariable Long id, Model model) {
        return downloadReceipt(id, model);
    }
}
