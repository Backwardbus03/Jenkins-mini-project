package com.rentreminder.app.repository;

import com.rentreminder.app.model.OwnerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OwnerAccountRepository extends JpaRepository<OwnerAccount, Long> {
    Optional<OwnerAccount> findByEmail(String email);
}
