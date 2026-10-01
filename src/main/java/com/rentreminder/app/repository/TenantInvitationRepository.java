package com.rentreminder.app.repository;

import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, Long> {

    List<TenantInvitation> findByTenantAccount(TenantAccount account);

    List<TenantInvitation> findByTenantEmail(String email);

    Optional<TenantInvitation> findByTenantAndStatus(Tenant tenant, String status);

    List<TenantInvitation> findByTenantAccountAndStatus(TenantAccount account, String status);
}
