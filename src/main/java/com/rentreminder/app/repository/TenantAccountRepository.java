package com.rentreminder.app.repository;

import com.rentreminder.app.model.TenantAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TenantAccountRepository extends JpaRepository<TenantAccount, Long> {
    Optional<TenantAccount> findByEmail(String email);
}
