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
        if (payment.getType() == null || payment.getType().isBlank()) {
            payment.setType("RENT");
        }
        if (payment.getTenant() != null && tenantInvitationService.isTenantLeaseExpired(payment.getTenant())) {
            throw new IllegalArgumentException("Cannot record payment: the lease duration for this tenant has expired.");
        }

        // Electricity calculations if applicable
        if (payment.isElectricity()) {
            if (payment.getPreviousReading() != null && payment.getCurrentReading() != null) {
                if (payment.getCurrentReading() < payment.getPreviousReading()) {
                    throw new IllegalArgumentException("Current meter reading cannot be less than previous meter reading.");
                }
                if (payment.getRatePerUnit() == null || payment.getRatePerUnit() <= 0) {
                    throw new IllegalArgumentException("Rate per unit must be greater than 0.");
                }
                double calculatedAmount = (payment.getCurrentReading() - payment.getPreviousReading()) * payment.getRatePerUnit();
                payment.setAmount(Math.round(calculatedAmount * 100.0) / 100.0);
            }
        }

        if (payment.getDueDate() == null && payment.getDate() != null) {
            payment.setDueDate(payment.getDate().plusDays(7));
        }

        Optional<Payment> existing = paymentRepository.findByTenantAndMonthAndYearAndType(
            payment.getTenant(), payment.getMonth(), payment.getYear(), payment.getType()
        );
        if (existing.isPresent() && !existing.get().getId().equals(payment.getId())) {
            throw new IllegalArgumentException(payment.getType() + " payment already exists for this tenant in the specified month and year.");
        }

        Payment saved = paymentRepository.save(payment);

        // Deduct from deposit ONLY when a RENT payment is recorded as overdue
        if ("Overdue".equalsIgnoreCase(saved.getStatus()) && saved.isRent() && saved.getAmount() != null) {
            tenantInvitationService.deductFromDeposit(saved.getTenant(), saved.getAmount());
        }
        return saved;
    }

    public Payment markPaymentPaidByTenant(Long paymentId, String upiReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.setStatus("Awaiting Confirmation");
        payment.setUpiReference(upiReference);
        return paymentRepository.save(payment);
    }

    public Payment confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.setStatus("Paid");
        return paymentRepository.save(payment);
    }

    public Payment rejectPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.setStatus("Pending");
        payment.setUpiReference(null);
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsByTenant(Tenant tenant) {
        return paymentRepository.findByTenant(tenant);
    }

    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id);
    }
}

