package com.rentreminder.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = true)
    private Tenant tenant;

    @Column(name = "recipient_role", nullable = false)
    private String recipientRole = "TENANT"; // "TENANT" or "OWNER"

    @Column(nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String message;

    @Column(nullable = false)
    private String type = "REMINDER"; // "REMINDER", "INFO", "WARNING", "SUCCESS", "DANGER"

    private String link;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = true)
    private Payment payment;

    public AppNotification() {
    }

    public AppNotification(Tenant tenant, String recipientRole, String title, String message, String type, String link, Payment payment) {
        this.tenant = tenant;
        this.recipientRole = recipientRole;
        this.title = title;
        this.message = message;
        this.type = type;
        this.link = link;
        this.payment = payment;
        this.readStatus = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}
