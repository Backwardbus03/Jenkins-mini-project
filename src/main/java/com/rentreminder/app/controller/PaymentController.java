package com.rentreminder.app.controller;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.service.PaymentService;
import com.rentreminder.app.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final TenantService tenantService;

    public PaymentController(PaymentService paymentService, TenantService tenantService) {
        this.paymentService = paymentService;
        this.tenantService = tenantService;
    }

    @GetMapping("/summary")
    public String showSummary(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payments/summary";
    }

    @GetMapping("/new")
    public String showPaymentForm(Model model) {
        model.addAttribute("payment", new Payment());
        model.addAttribute("tenants", tenantService.getAllTenants());
        return "payments/form";
    }

    @PostMapping
    public String savePayment(@Valid @ModelAttribute("payment") Payment payment, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tenants", tenantService.getAllTenants());
            return "payments/form";
        }
        try {
            paymentService.savePayment(payment);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("month", "error.payment", e.getMessage());
            model.addAttribute("tenants", tenantService.getAllTenants());
            return "payments/form";
        }
        return "redirect:/payments/summary";
    }

    @GetMapping("/receipt")
    public String downloadReceipt(Model model) {
        // Just returns a printable HTML view for MVP
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payments/receipt";
    }
}
