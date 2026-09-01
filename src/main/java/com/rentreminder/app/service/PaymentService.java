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

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment savePayment(Payment payment) {
        Optional<Payment> existing = paymentRepository.findByTenantAndMonthAndYear(
            payment.getTenant(), payment.getMonth(), payment.getYear()
        );
        if (existing.isPresent() && !existing.get().getId().equals(payment.getId())) {
            throw new IllegalArgumentException("Payment already exists for this tenant in the specified month and year.");
        }
        return paymentRepository.save(payment);
    }
}
