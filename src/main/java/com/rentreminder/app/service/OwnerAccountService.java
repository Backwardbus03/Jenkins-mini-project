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

    /** Seeds the default owner if none exist yet. */
    public void seedDefaultOwner(String name, String email, String rawPassword) {
        if (ownerAccountRepository.findByEmail(email).isEmpty()) {
            ownerAccountRepository.save(
                new OwnerAccount(name, email, passwordEncoder.encode(rawPassword))
            );
        }
    }
}
