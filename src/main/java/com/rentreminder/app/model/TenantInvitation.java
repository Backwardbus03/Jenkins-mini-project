package com.rentreminder.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Links an owner-side Tenant record with a TenantAccount (the tenant's login).
 * Tracks the invitation lifecycle (PENDING → APPROVED/REJECTED) and
 * the deposit balance (deducted when rent is marked overdue by the owner).
 */
@Entity
@Table(name = "tenant_invitations")
public class TenantInvitation {

    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The owner-side tenant record (apt, name etc.) */
    @ManyToOne
    private Tenant tenant;

    /** Resolved when the tenant creates / logs into their account */
    @ManyToOne
    private TenantAccount tenantAccount;

    /** Email used to match invitation when tenant registers later */
    private String tenantEmail;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    /** Deposit amount filled in by the tenant after approval */
    private Double depositAmount;

    /** Running balance; starts equal to depositAmount, deducted on each OVERDUE payment */
    private Double depositBalance;

    private LocalDate rentalStartDate;
    private LocalDate rentalEndDate;
    private LocalDate createdAt;

    public TenantInvitation() {}

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public TenantAccount getTenantAccount() { return tenantAccount; }
    public void setTenantAccount(TenantAccount tenantAccount) { this.tenantAccount = tenantAccount; }

    public String getTenantEmail() { return tenantEmail; }
    public void setTenantEmail(String tenantEmail) { this.tenantEmail = tenantEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Double depositAmount) { this.depositAmount = depositAmount; }

    public Double getDepositBalance() { return depositBalance; }
    public void setDepositBalance(Double depositBalance) { this.depositBalance = depositBalance; }

    public LocalDate getRentalStartDate() { return rentalStartDate; }
    public void setRentalStartDate(LocalDate rentalStartDate) { this.rentalStartDate = rentalStartDate; }

    public LocalDate getRentalEndDate() { return rentalEndDate; }
    public void setRentalEndDate(LocalDate rentalEndDate) { this.rentalEndDate = rentalEndDate; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    /** Returns true if the approved rental end date has passed today. */
    public boolean isExpired() {
        return STATUS_APPROVED.equalsIgnoreCase(this.status)
                && this.rentalEndDate != null
                && this.rentalEndDate.isBefore(LocalDate.now());
    }
}
