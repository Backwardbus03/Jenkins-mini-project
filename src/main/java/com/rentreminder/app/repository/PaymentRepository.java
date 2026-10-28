package com.rentreminder.app.repository;

import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTenantAndMonthAndYear(Tenant tenant, Integer month, Integer year);
    Optional<Payment> findByTenantAndMonthAndYearAndType(Tenant tenant, Integer month, Integer year, String type);
    List<Payment> findByTenant(Tenant tenant);
    List<Payment> findByTenantAndStatus(Tenant tenant, String status);
    List<Payment> findByStatusIn(List<String> statuses);
}
