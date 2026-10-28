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

    public PaymentController(PaymentService paymentService,
                             TenantService tenantService,
                             TenantInvitationService tenantInvitationService,
                             com.rentreminder.app.service.ReminderService reminderService) {
        this.paymentService = paymentService;
        this.tenantService = tenantService;
        this.tenantInvitationService = tenantInvitationService;
        this.reminderService = reminderService;
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/payments/summary";
    }

    @GetMapping({"/summary", "/list"})
    public String showSummary(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
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
            paymentService.confirmPayment(id);
            redirectAttributes.addFlashAttribute("success", "Payment confirmed and verified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/payments/summary";
    }

    @PostMapping("/{id}/reject")
    public String rejectPayment(@PathVariable Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            paymentService.rejectPayment(id);
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
            if (sent) {
                redirectAttributes.addFlashAttribute("success", "Reminder email sent to tenant.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Reminder attempted, but delivery was skipped or failed (check SMTP settings).");
            }
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
