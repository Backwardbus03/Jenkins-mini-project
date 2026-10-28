package com.rentreminder.app.service;

import com.rentreminder.app.model.OwnerAccount;
import com.rentreminder.app.repository.OwnerAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OwnerAccountService {

    private final OwnerAccountRepository ownerAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public OwnerAccountService(OwnerAccountRepository ownerAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.ownerAccountRepository = ownerAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<OwnerAccount> findByEmail(String email) {
        return ownerAccountRepository.findByEmail(email);
    }

    /** Seeds the default owner if none exist yet, or updates UPI ID if not set. */
    public void seedDefaultOwner(String name, String email, String rawPassword, String upiId) {
        Optional<OwnerAccount> existing = ownerAccountRepository.findByEmail(email);
        if (existing.isEmpty()) {
            ownerAccountRepository.save(
                    new OwnerAccount(name, email, passwordEncoder.encode(rawPassword), upiId));
        } else {
            OwnerAccount owner = existing.get();
            if (upiId != null && !upiId.isBlank() && !upiId.equals(owner.getUpiId())) {
                owner.setUpiId(upiId);
                ownerAccountRepository.save(owner);
            }
        }
    }

    public String getDefaultOwnerUpi() {
        return ownerAccountRepository.findAll().stream()
                .filter(o -> o.getUpiId() != null && !o.getUpiId().isBlank())
                .map(OwnerAccount::getUpiId)
                .findFirst()
                .orElse("sumedhhadkar@oksbi");
    }

    public String getDefaultOwnerName() {
        return ownerAccountRepository.findAll().stream()
                .map(OwnerAccount::getName)
                .findFirst()
                .orElse("Property Owner");
    }
}
