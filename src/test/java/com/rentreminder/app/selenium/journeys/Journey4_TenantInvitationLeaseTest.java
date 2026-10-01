package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.pages.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User Journey 4: Tenant Invitation Review & Acceptance, Lease Duration, and Security Deposit Configuration.
 */
public class Journey4_TenantInvitationLeaseTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Journey 4.1: Verify end-to-end invitation acceptance, deposit setup, and active lease confirmation")
    void testTenantInvitationReviewAcceptanceAndDepositSetup() {
        long ts = System.currentTimeMillis();
        String tenantName = "Meera Sen";
        String tenantEmail = "meera." + ts + "@example.com";
        String aptNumber = "501";
        String tenantPassword = "TenantSecurePass789";

        // Step 1: Owner onboards the tenant
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage ownerTenantsPage = new OwnerTenantsPage(driver);
        ownerTenantsPage.clickAddTenant();

        AddTenantPage addTenantPage = new AddTenantPage(driver);
        addTenantPage.saveTenant(tenantName, tenantEmail, "+91 91234 56789", aptNumber);
        assertTrue(waitForUrlContains("/tenants"));
        assertTrue(ownerTenantsPage.hasTenant(tenantEmail), "Tenant should be added to owner directory");

        ownerTenantsPage.clickSignOut();

        // Step 2: Tenant creates portal account with the same email
        loginPage.clickRegisterLink();
        TenantRegistrationPage regPage = new TenantRegistrationPage(driver);
        regPage.register(tenantName, tenantEmail, "+91 91234 56789", tenantPassword, tenantPassword);

        // Step 3: Tenant logs into portal dashboard
        loginPage.loginAs(tenantEmail, tenantPassword);
        TenantDashboardPage dashboard = new TenantDashboardPage(driver);
        assertTrue(dashboard.isDisplayed(), "Tenant dashboard must be displayed");
        assertTrue(dashboard.hasPendingInvitation(), "Pending invitation card must be visible to tenant");

        // Step 4: Tenant approves invitation, specifies security deposit and lease dates
        double depositAmount = 25000.00;
        String startDate = "2026-01-01";
        String endDate = "2027-01-01";
        dashboard.approveInvitation(depositAmount, startDate, endDate);

        // Assert lease approval success and dashboard cards
        assertTrue(waitForUrlContains("/portal/dashboard"), "Should redirect back to dashboard after approval");
        assertTrue(dashboard.hasSuccessAlert(), "Success alert must be shown after approval");
        assertTrue(dashboard.getSuccessAlertText().contains("Invitation approved"),
                "Alert must confirm 'Invitation approved! Your rental is now active.'");
        assertTrue(dashboard.hasActiveLease(), "Active lease card must now be rendered on dashboard");

        String leaseText = dashboard.getLeaseCardText();
        assertTrue(leaseText.toUpperCase().contains("APT " + aptNumber), "Lease card must show Apartment " + aptNumber);
        assertTrue(leaseText.toUpperCase().contains("ACTIVE"), "Lease card must indicate Active status");

        String depositBalance = dashboard.getDepositBalanceText();
        assertTrue(depositBalance.contains("25000"), "Deposit balance must reflect configured ₹25000");

        dashboard.clickSignOut();

        // Step 5: Verify Owner's directory reflects the Accepted lease status
        loginPage.loginAs("owner@rentportal.com", "owner123");
        assertTrue(ownerTenantsPage.isDisplayed());

        String updatedRow = ownerTenantsPage.getTenantRowDetailsByEmail(tenantEmail);
        assertNotNull(updatedRow);
        assertTrue(updatedRow.toUpperCase().contains("ACCEPTED"), "Portal status in owner directory must update to 'Accepted'");
        assertTrue(updatedRow.contains("25000") || updatedRow.contains("25,000"),
                "Deposit amount must be visible in owner directory");
    }
}
