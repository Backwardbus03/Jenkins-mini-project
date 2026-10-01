package com.rentreminder.app.service;

import com.rentreminder.app.model.TenantAccount;
import com.rentreminder.app.model.TenantInvitation;
import com.rentreminder.app.repository.TenantAccountRepository;
import com.rentreminder.app.repository.TenantInvitationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TenantAccountService {

    private final TenantAccountRepository tenantAccountRepository;
    private final TenantInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantAccountService(TenantAccountRepository tenantAccountRepository,
                                TenantInvitationRepository invitationRepository,
                                PasswordEncoder passwordEncoder) {
        this.tenantAccountRepository = tenantAccountRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Register a new tenant account and link any existing pending invitations. */
    @Transactional
    public TenantAccount register(String name, String email, String phone, String rawPassword) {
        if (tenantAccountRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        TenantAccount account = new TenantAccount(name, email, phone,
                passwordEncoder.encode(rawPassword));
        account = tenantAccountRepository.save(account);

        // Link any pending invitations sent to this email
        List<TenantInvitation> pending = invitationRepository.findByTenantEmail(email);
        for (TenantInvitation inv : pending) {
            if (inv.getTenantAccount() == null) {
                inv.setTenantAccount(account);
                invitationRepository.save(inv);
            }
        }
        return account;
    }

    public Optional<TenantAccount> findByEmail(String email) {
        return tenantAccountRepository.findByEmail(email);
    }
}
