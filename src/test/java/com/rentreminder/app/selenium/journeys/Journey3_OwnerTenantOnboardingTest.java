package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.pages.AddTenantPage;
import com.rentreminder.app.selenium.pages.LoginPage;
import com.rentreminder.app.selenium.pages.OwnerTenantsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User Journey 3: Owner Tenant Onboarding & Automated Invitation Generation.
 */
public class Journey3_OwnerTenantOnboardingTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Journey 3.1: Verify Owner can onboard a new tenant and trigger automatic pending invitation")
    void testOwnerAddTenantAndAutomatedInvitationCreation() {
        // 1. Authenticate as Owner
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage tenantsPage = new OwnerTenantsPage(driver);
        assertTrue(tenantsPage.isDisplayed(), "Owner directory must be visible");

        // 2. Open Add Tenant Form
        tenantsPage.clickAddTenant();
        AddTenantPage addPage = new AddTenantPage(driver);
        assertTrue(addPage.isDisplayed(), "Add Tenant form must load");

        // 3. Fill and Save Tenant Details
        long ts = System.currentTimeMillis();
        String tenantName = "Karan Kapoor";
        String tenantEmail = "karan." + ts + "@example.com";
        String aptNumber = "302";

        addPage.saveTenant(tenantName, tenantEmail, "+91 99887 76655", aptNumber);

        // 4. Assert Redirection to Directory & Verify Row Information
        assertTrue(waitForUrlContains("/tenants"), "Should redirect back to Tenants Directory");
        assertTrue(tenantsPage.hasTenant(tenantEmail), "Newly added tenant must appear in Active list by email");

        String rowDetails = tenantsPage.getTenantRowDetailsByEmail(tenantEmail);
        assertNotNull(rowDetails, "Tenant row details must be retrieved");
        assertTrue(rowDetails.contains(tenantName), "Row must display tenant full name");
        assertTrue(rowDetails.toUpperCase().contains("APT " + aptNumber), "Row must display assigned apartment number");
        assertTrue(rowDetails.contains(tenantEmail), "Row must display linked email address");
        assertTrue(rowDetails.toUpperCase().contains("PENDING APPROVAL"),
                "Portal status must indicate Pending Approval");
        assertTrue(rowDetails.contains("+ Add Payment") || rowDetails.contains("Add Payment"),
                "Action button for recording payment must be present");
    }
}
