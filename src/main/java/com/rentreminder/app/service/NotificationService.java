package com.rentreminder.app.service;

import com.rentreminder.app.model.AppNotification;
import com.rentreminder.app.model.Payment;
import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create and store an in-app UI notification for a tenant.
     */
    @Transactional
    public AppNotification notifyTenant(Tenant tenant, String title, String message, String type, String link, Payment payment) {
        AppNotification notification = new AppNotification(tenant, "TENANT", title, message, type, link, payment);
        AppNotification saved = notificationRepository.save(notification);
        log.info("Created UI notification #{} for tenant {} (Apt {}): '{}'",
                saved.getId(), tenant != null ? tenant.getName() : "All",
                tenant != null ? tenant.getApartmentNumber() : "-", title);
        return saved;
    }

    /**
     * Create and store an in-app UI notification for the owner/landlord.
     */
    @Transactional
    public AppNotification notifyOwner(String title, String message, String type, String link, Payment payment) {
        AppNotification notification = new AppNotification(null, "OWNER", title, message, type, link, payment);
        AppNotification saved = notificationRepository.save(notification);
        log.info("Created UI notification #{} for Owner: '{}'", saved.getId(), title);
        return saved;
    }

    public List<AppNotification> getNotificationsForTenant(Tenant tenant) {
        return notificationRepository.findByTenantOrderByCreatedAtDesc(tenant);
    }

    public List<AppNotification> getUnreadForTenant(Tenant tenant) {
        return notificationRepository.findByTenantAndReadStatusFalseOrderByCreatedAtDesc(tenant);
    }

    public long getUnreadCountForTenant(Tenant tenant) {
        return notificationRepository.countByTenantAndReadStatusFalse(tenant);
    }

    public List<AppNotification> getNotificationsForOwner() {
        return notificationRepository.findByRecipientRoleOrderByCreatedAtDesc("OWNER");
    }

    public long getUnreadCountForOwner() {
        return notificationRepository.countByRecipientRoleAndReadStatusFalse("OWNER");
    }

    @Transactional
    public boolean markAsRead(Long notificationId) {
        Optional<AppNotification> opt = notificationRepository.findById(notificationId);
        if (opt.isPresent()) {
            AppNotification n = opt.get();
            n.setReadStatus(true);
            notificationRepository.save(n);
            return true;
        }
        return false;
    }

    @Transactional
    public void markAllAsReadForTenant(Tenant tenant) {
        List<AppNotification> unread = notificationRepository.findByTenantAndReadStatusFalseOrderByCreatedAtDesc(tenant);
        unread.forEach(n -> n.setReadStatus(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void markAllAsReadForOwner() {
        List<AppNotification> unread = notificationRepository.findByRecipientRoleAndReadStatusFalseOrderByCreatedAtDesc("OWNER");
        unread.forEach(n -> n.setReadStatus(true));
        notificationRepository.saveAll(unread);
    }
}

