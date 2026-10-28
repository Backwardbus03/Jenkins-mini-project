package com.rentreminder.app.config;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.repository.PaymentRepository;
import com.rentreminder.app.repository.TenantAccountRepository;
import com.rentreminder.app.repository.TenantInvitationRepository;
import com.rentreminder.app.repository.TenantRepository;
import com.rentreminder.app.service.NotificationService;
import com.rentreminder.app.service.OwnerAccountService;
import com.rentreminder.app.service.TenantAccountService;
import com.rentreminder.app.service.TenantInvitationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds default accounts and sample electricity/rent bills on startup.
 * Owner: owner@rentportal.com / owner123
 * Tenant: tenant@rentportal.com / tenant123
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final OwnerAccountService ownerAccountService;
    private final TenantRepository tenantRepository;
    private final TenantAccountRepository tenantAccountRepository;
    private final TenantAccountService tenantAccountService;
    private final TenantInvitationService tenantInvitationService;
    private final TenantInvitationRepository invitationRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    public DataSeeder(OwnerAccountService ownerAccountService,
                      TenantRepository tenantRepository,
                      TenantAccountRepository tenantAccountRepository,
                      TenantAccountService tenantAccountService,
                      TenantInvitationService tenantInvitationService,
                      TenantInvitationRepository invitationRepository,
                      PaymentRepository paymentRepository,
                      NotificationService notificationService) {
        this.ownerAccountService = ownerAccountService;
        this.tenantRepository = tenantRepository;
        this.tenantAccountRepository = tenantAccountRepository;
        this.tenantAccountService = tenantAccountService;
        this.tenantInvitationService = tenantInvitationService;
        this.invitationRepository = invitationRepository;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        // Ensure default owner account exists with UPI ID
        ownerAccountService.seedDefaultOwner("Property Owner", "owner@rentportal.com", "owner123",
                "sumedhhadkar@oksbi");

        // Seed demo tenant and pending electricity bill if none exists
        String demoEmail = "tenant@rentportal.com";
        if (tenantRepository.findByEmail(demoEmail).isEmpty()) {
            Tenant tenant = new Tenant("Rahul Sharma", demoEmail, "9876543210", "101");
            tenant = tenantRepository.save(tenant);

            TenantAccount account;
            if (tenantAccountRepository.findByEmail(demoEmail).isEmpty()) {
                account = tenantAccountService.register("Rahul Sharma", demoEmail, "9876543210", "tenant123");
            } else {
                account = tenantAccountRepository.findByEmail(demoEmail).get();
            }

            // Create and approve invitation
            TenantInvitation invite = tenantInvitationService.createInvitation(tenant, demoEmail);
            invite.setStatus(TenantInvitation.STATUS_APPROVED);
            invite.setTenantAccount(account);
            invite.setDepositAmount(25000.0);
            invite.setDepositBalance(25000.0);
            invite.setRentalStartDate(LocalDate.of(2026, 1, 1));
            invite.setRentalEndDate(LocalDate.of(2027, 1, 1));
            invitationRepository.save(invite);

            LocalDate today = LocalDate.now();

            // Seed sample pending electricity bill with UPI payment ready
            Payment elecBill = new Payment();
            elecBill.setTenant(tenant);
            elecBill.setType("ELECTRICITY");
            elecBill.setPreviousReading(1200.0);
            elecBill.setCurrentReading(1380.0);
            elecBill.setRatePerUnit(10.0);
            elecBill.setAmount(1800.0);
            elecBill.setDate(today);
            elecBill.setDueDate(today.plusDays(3));
            elecBill.setMonth(today.getMonthValue());
            elecBill.setYear(today.getYear());
            elecBill.setStatus("Pending");
            elecBill = paymentRepository.save(elecBill);

            // Seed in-app UI reminder notification for the tenant
            notificationService.notifyTenant(
                    tenant,
                    "⚡ Electricity Bill Reminder: ₹1,800.00 due soon",
                    String.format(
                            "Your Electricity Bill for Apt 101 (Period %d/%d) of ₹1,800.00 is due on %s. Scan your UPI QR code directly on your dashboard to complete the payment.",
                            today.getMonthValue(), today.getYear(), today.plusDays(3)),
                    "WARNING",
                    "/portal/dashboard#payments",
                    elecBill);

            // Seed sample past rent payment
            Payment pastRent = new Payment();
            pastRent.setTenant(tenant);
            pastRent.setType("RENT");
            pastRent.setAmount(15000.0);
            pastRent.setDate(today.minusMonths(1));
            pastRent.setDueDate(today.minusMonths(1).plusDays(5));
            pastRent.setMonth(today.minusMonths(1).getMonthValue());
            pastRent.setYear(today.minusMonths(1).getYear());
            pastRent.setStatus("Paid");
            paymentRepository.save(pastRent);
        }
    }
}
