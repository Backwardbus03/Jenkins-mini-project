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

    public PaymentController(PaymentService paymentService,
                             TenantService tenantService,
                             TenantInvitationService tenantInvitationService) {
        this.paymentService = paymentService;
        this.tenantService = tenantService;
        this.tenantInvitationService = tenantInvitationService;
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
    public String showPaymentForm(Model model) {
        model.addAttribute("payment", new Payment());
        model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
        return "payments/form";
    }

    @PostMapping({"", "/"})
    public String savePayment(@Valid @ModelAttribute("payment") Payment payment, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
            return "payments/form";
        }
        try {
            paymentService.savePayment(payment);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("month", "error.payment", e.getMessage());
            model.addAttribute("tenants", tenantInvitationService.filterActiveTenants(tenantService.getAllTenants()));
            return "payments/form";
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
