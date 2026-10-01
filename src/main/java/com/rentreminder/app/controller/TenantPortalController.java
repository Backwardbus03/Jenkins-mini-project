package com.rentreminder.app.controller;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.repository.PaymentRepository;
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

    public TenantPortalController(TenantAccountService accountService,
                                  TenantInvitationService invitationService,
                                  PaymentRepository paymentRepository) {
        this.accountService = accountService;
        this.invitationService = invitationService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping({"", "/"})
    public String portalHome() {
        return "redirect:/portal/dashboard";
    }

    // ── Registration ──────────────────────────────────────────────────────

    @GetMapping({"/register", "/signup"})
    public String showRegisterForm() {
        return "portal/register";
    }

    @PostMapping({"/register", "/signup"})
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

    @GetMapping({"/dashboard", "/home"})
    public String dashboard(Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null) return "redirect:/login";

        List<TenantInvitation> pending  = invitationService.getPendingForAccount(account);
        List<TenantInvitation> approved = invitationService.getApprovedForAccount(account);

        List<Payment> allPayments = new ArrayList<>();
        List<Payment> overduePayments = new ArrayList<>();

        // For each approved lease, fetch all payment history
        approved.forEach(invite -> {
            var tenantPayments = paymentRepository.findByTenant(invite.getTenant());
            allPayments.addAll(tenantPayments);

            var tenantOverdue = tenantPayments.stream()
                    .filter(p -> "Overdue".equalsIgnoreCase(p.getStatus()))
                    .toList();
            overduePayments.addAll(tenantOverdue);
            model.addAttribute("overduePayments_" + invite.getId(), tenantOverdue);
        });

        model.addAttribute("account",         account);
        model.addAttribute("pending",         pending);
        model.addAttribute("approved",        approved);
        model.addAttribute("allPayments",     allPayments);
        model.addAttribute("overduePayments", overduePayments);
        return "portal/dashboard";
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

    @GetMapping({"/receipt/{id}", "/receipts/{id}"})
    public String viewReceipt(@PathVariable Long id, Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null) return "redirect:/login";

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

    @GetMapping({"/receipt", "/receipts"})
    public String viewAllReceipts(Authentication auth, Model model) {
        TenantAccount account = resolveAccount(auth);
        if (account == null) return "redirect:/login";

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
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        Optional<TenantAccount> opt = accountService.findByEmail(email);
        return opt.orElse(null);
    }
}
