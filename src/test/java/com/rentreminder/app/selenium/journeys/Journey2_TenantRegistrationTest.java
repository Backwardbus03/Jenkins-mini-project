package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.pages.LoginPage;
import com.rentreminder.app.selenium.pages.TenantDashboardPage;
import com.rentreminder.app.selenium.pages.TenantRegistrationPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User Journey 2: Tenant Self-Service Registration, Input Validation, and Dashboard Access.
 */
public class Journey2_TenantRegistrationTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Journey 2.1: Verify error validation alert when registration passwords do not match")
    void testTenantRegistrationPasswordMismatch() {
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.clickRegisterLink();

        TenantRegistrationPage regPage = new TenantRegistrationPage(driver);
        assertTrue(regPage.isDisplayed(), "Tenant registration page should be open");

        long ts = System.currentTimeMillis();
        regPage.register("Aarav Test", "aarav." + ts + "@example.com", "9812345678", "ValidPass123", "MismatchedPass999");

        assertTrue(regPage.isErrorAlertDisplayed(), "Error banner should be displayed for mismatched passwords");
        assertTrue(regPage.getErrorMessage().contains("Passwords do not match"),
                "Error banner should state 'Passwords do not match.'");
    }

    @Test
    @DisplayName("Journey 2.2: Verify complete tenant registration lifecycle and first-time dashboard access")
    void testTenantRegistrationSuccessAndDashboardLogin() {
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.clickRegisterLink();

        TenantRegistrationPage regPage = new TenantRegistrationPage(driver);
        assertTrue(regPage.isDisplayed());

        long ts = System.currentTimeMillis();
        String tenantEmail = "priya." + ts + "@example.com";
        String tenantName = "Priya Nair";
        String password = "StrongPassword123";

        // Fill and submit valid registration
        regPage.register(tenantName, tenantEmail, "+91 98765 00001", password, password);

        // Should redirect to login page with success notification
        assertTrue(loginPage.isDisplayed(), "Should be redirected back to Login page");
        assertTrue(loginPage.isSuccessAlertDisplayed(), "Success banner should be visible");
        assertTrue(loginPage.getSuccessMessage().contains("Account created"),
                "Banner should confirm account creation");

        // Log in using new tenant account
        loginPage.loginAs(tenantEmail, password);

        // Verify Tenant Dashboard
        TenantDashboardPage dashboard = new TenantDashboardPage(driver);
        assertTrue(dashboard.isDisplayed(), "Tenant Dashboard should be displayed");
        assertTrue(driver.getCurrentUrl().contains("/portal/dashboard"), "URL should be /portal/dashboard");
        assertTrue(dashboard.getNavbarWelcomeText().contains(tenantName),
                "Navbar should greet tenant by name: " + tenantName);
        assertTrue(dashboard.hasEmptyState(), "Dashboard should indicate empty state when no invitations exist yet");
        assertTrue(dashboard.getEmptyStateHeading().contains("No invitations yet"),
                "Empty state should show 'No invitations yet'");
    }
}
