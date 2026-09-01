package com.rentreminder.app.repository;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTenantAndMonthAndYear(Tenant tenant, Integer month, Integer year);
}
