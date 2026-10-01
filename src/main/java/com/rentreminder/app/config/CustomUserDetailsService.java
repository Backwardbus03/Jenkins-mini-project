package com.rentreminder.app.config;

import com.rentreminder.app.repository.OwnerAccountRepository;
import com.rentreminder.app.repository.TenantAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Unified UserDetailsService that looks up both owner and tenant accounts.
 * The "username" in Spring Security context is the account's email address.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final OwnerAccountRepository ownerRepo;
    private final TenantAccountRepository tenantRepo;

    public CustomUserDetailsService(OwnerAccountRepository ownerRepo,
                                    TenantAccountRepository tenantRepo) {
        this.ownerRepo = ownerRepo;
        this.tenantRepo = tenantRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Check owner accounts first
        var owner = ownerRepo.findByEmail(email.toLowerCase().trim());
        if (owner.isPresent()) {
            return User.builder()
                    .username(owner.get().getEmail())
                    .password(owner.get().getPasswordHash())
                    .roles("OWNER")
                    .build();
        }

        // Check tenant accounts
        var tenant = tenantRepo.findByEmail(email.toLowerCase().trim());
        if (tenant.isPresent()) {
            return User.builder()
                    .username(tenant.get().getEmail())
                    .password(tenant.get().getPasswordHash())
                    .roles("TENANT")
                    .build();
        }

        throw new UsernameNotFoundException("No account found for email: " + email);
    }
}
