package com.rentreminder.app.config;

import com.rentreminder.app.service.OwnerAccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a default owner account on first startup.
 * Credentials: owner@rentportal.com / owner123
 * Change the password after first login in production!
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final OwnerAccountService ownerAccountService;

    public DataSeeder(OwnerAccountService ownerAccountService) {
        this.ownerAccountService = ownerAccountService;
    }

    @Override
    public void run(String... args) {
        ownerAccountService.seedDefaultOwner(
                "Property Owner",
                "owner@rentportal.com",
                "owner123"
        );
    }
}
