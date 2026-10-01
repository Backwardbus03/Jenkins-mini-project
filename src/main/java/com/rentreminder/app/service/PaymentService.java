package com.rentreminder.app.service;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TenantInvitationService tenantInvitationService;

    public PaymentService(PaymentRepository paymentRepository,
                          TenantInvitationService tenantInvitationService) {
        this.paymentRepository = paymentRepository;
        this.tenantInvitationService = tenantInvitationService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment savePayment(Payment payment) {
        if (payment.getTenant() != null && tenantInvitationService.isTenantLeaseExpired(payment.getTenant())) {
            throw new IllegalArgumentException("Cannot record rent payment: the lease duration for this tenant has expired.");
        }
        Optional<Payment> existing = paymentRepository.findByTenantAndMonthAndYear(
            payment.getTenant(), payment.getMonth(), payment.getYear()
        );
        if (existing.isPresent() && !existing.get().getId().equals(payment.getId())) {
            throw new IllegalArgumentException("Payment already exists for this tenant in the specified month and year.");
        }
        Payment saved = paymentRepository.save(payment);

        // Deduct from deposit when a payment is recorded as overdue
        if ("Overdue".equalsIgnoreCase(saved.getStatus()) && saved.getAmount() != null) {
            tenantInvitationService.deductFromDeposit(saved.getTenant(), saved.getAmount());
        }
        return saved;
    }

    public List<Payment> getPaymentsByTenant(Tenant tenant) {
        return paymentRepository.findByTenant(tenant);
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }
}

