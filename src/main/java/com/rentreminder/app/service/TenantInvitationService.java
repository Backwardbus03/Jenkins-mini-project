package com.rentreminder.app.service;

import com.rentreminder.app.model.Tenant;
import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.repository.TenantAccountRepository;
import com.rentreminder.app.repository.TenantInvitationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantInvitationService {

    private final TenantInvitationRepository invitationRepository;
    private final TenantAccountRepository tenantAccountRepository;

    public TenantInvitationService(TenantInvitationRepository invitationRepository,
                                   TenantAccountRepository tenantAccountRepository) {
        this.invitationRepository = invitationRepository;
        this.tenantAccountRepository = tenantAccountRepository;
    }

    /**
     * Called by the owner when adding a tenant. Creates a PENDING invitation.
     * If a TenantAccount with the given email already exists, links it immediately.
     */
    @Transactional
    public TenantInvitation createInvitation(Tenant tenant, String tenantEmail) {
        TenantInvitation invite = new TenantInvitation();
        invite.setTenant(tenant);
        invite.setTenantEmail(tenantEmail.toLowerCase().trim());
        invite.setStatus(TenantInvitation.STATUS_PENDING);
        invite.setCreatedAt(LocalDate.now());

        // Link immediately if account already exists
        tenantAccountRepository.findByEmail(tenantEmail.toLowerCase().trim())
                .ifPresent(invite::setTenantAccount);

        return invitationRepository.save(invite);
    }

    /** Returns the latest invitation for each tenant mapped by tenantId. */
    public Map<Long, TenantInvitation> getLatestInvitationsByTenant() {
        List<TenantInvitation> all = invitationRepository.findAll();
        Map<Long, TenantInvitation> map = new HashMap<>();
        for (TenantInvitation inv : all) {
            if (inv.getTenant() != null) {
                Long tenantId = inv.getTenant().getId();
                TenantInvitation existing = map.get(tenantId);
                if (existing == null || (inv.getId() != null && inv.getId() > existing.getId())) {
                    map.put(tenantId, inv);
                }
            }
        }
        return map;
    }

    /** Returns true if the tenant has an approved invitation whose lease duration has passed. */
    public boolean isTenantLeaseExpired(Tenant tenant) {
        if (tenant == null) return false;
        return invitationRepository.findByTenantAndStatus(tenant, TenantInvitation.STATUS_APPROVED)
                .map(TenantInvitation::isExpired)
                .orElse(false);
    }

    /** Filters a list of tenants to only those who are active (not expired). */
    public List<Tenant> filterActiveTenants(List<Tenant> tenants) {
        Map<Long, TenantInvitation> latestMap = getLatestInvitationsByTenant();
        return tenants.stream()
                .filter(t -> {
                    TenantInvitation inv = latestMap.get(t.getId());
                    return inv == null || !inv.isExpired();
                })
                .toList();
    }

    /** Returns all invitations (any status) for a given tenant account. */
    public List<TenantInvitation> getAllForAccount(TenantAccount account) {
        return invitationRepository.findByTenantAccount(account);
    }

    /** Returns only PENDING invitations for the given account. */
    public List<TenantInvitation> getPendingForAccount(TenantAccount account) {
        return invitationRepository.findByTenantAccountAndStatus(account,
                TenantInvitation.STATUS_PENDING);
    }

    /** Returns only APPROVED invitations for the given account. */
    public List<TenantInvitation> getApprovedForAccount(TenantAccount account) {
        return invitationRepository.findByTenantAccountAndStatus(account,
                TenantInvitation.STATUS_APPROVED);
    }

    /** Tenant approves the invitation and fills in deposit + rental dates. */
    @Transactional
    public TenantInvitation approve(Long inviteId, Double depositAmount,
                                    LocalDate startDate, LocalDate endDate) {
        TenantInvitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
        invite.setStatus(TenantInvitation.STATUS_APPROVED);
        invite.setDepositAmount(depositAmount);
        invite.setDepositBalance(depositAmount);
        invite.setRentalStartDate(startDate);
        invite.setRentalEndDate(endDate);
        return invitationRepository.save(invite);
    }

    /** Tenant rejects the invitation. */
    @Transactional
    public TenantInvitation reject(Long inviteId) {
        TenantInvitation invite = invitationRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));
        invite.setStatus(TenantInvitation.STATUS_REJECTED);
        return invitationRepository.save(invite);
    }

    /**
     * Called when the owner marks a payment as OVERDUE.
     * Deducts the rent amount from the deposit balance of the tenant's active invitation.
     */
    @Transactional
    public void deductFromDeposit(Tenant tenant, Double amount) {
        invitationRepository.findByTenantAndStatus(tenant, TenantInvitation.STATUS_APPROVED)
                .ifPresent(invite -> {
                    double current = invite.getDepositBalance() != null
                            ? invite.getDepositBalance() : 0.0;
                    invite.setDepositBalance(Math.max(0.0, current - amount));
                    invitationRepository.save(invite);
                });
    }
}
