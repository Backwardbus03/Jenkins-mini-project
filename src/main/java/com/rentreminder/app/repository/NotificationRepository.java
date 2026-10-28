package com.rentreminder.app.repository;

import com.rentreminder.app.model.AppNotification;
import com.rentreminder.app.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByTenantOrderByCreatedAtDesc(Tenant tenant);

    List<AppNotification> findByTenantAndReadStatusFalseOrderByCreatedAtDesc(Tenant tenant);

    long countByTenantAndReadStatusFalse(Tenant tenant);

    List<AppNotification> findByRecipientRoleOrderByCreatedAtDesc(String recipientRole);

    List<AppNotification> findByRecipientRoleAndReadStatusFalseOrderByCreatedAtDesc(String recipientRole);

    long countByRecipientRoleAndReadStatusFalse(String recipientRole);
}
