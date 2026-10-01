package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.pages.LoginPage;
import com.rentreminder.app.selenium.pages.OwnerTenantsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * User Journey 1: Owner Authentication, Role-based Redirection, and Security Access Control.
 */
public class Journey1_OwnerAuthenticationTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Journey 1.1: Verify error alert when invalid owner credentials are submitted")
    void testOwnerLoginInvalidCredentials() {
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        assertTrue(loginPage.isDisplayed(), "Login page should be displayed");

        loginPage.loginAs("unregistered.owner@rentportal.com", "WrongPassword999");

        assertTrue(waitForUrlContains("error"), "URL should contain error parameter after failed login");
        assertTrue(loginPage.isErrorAlertDisplayed(), "Error alert should be visible");
        assertTrue(loginPage.getErrorMessage().contains("Invalid email or password"),
                "Alert message should indicate invalid credentials");
    }

    @Test
    @DisplayName("Journey 1.2: Verify successful login with default owner credentials and redirection to Tenants Directory")
    void testOwnerLoginValidCredentialsAndRedirection() {
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage tenantsPage = new OwnerTenantsPage(driver);
        assertTrue(tenantsPage.isDisplayed(), "Should be redirected to Owner Tenants Directory");
        assertTrue(waitForUrlContains("/tenants"), "Current URL should end with /tenants");
        assertTrue(tenantsPage.getPageTitleText().contains("Tenants Directory"),
                "Directory title should be visible");
    }

    @Test
    @DisplayName("Journey 1.3: Verify owner sign-out terminates session and displays logout confirmation alert")
    void testOwnerSignOutFlow() {
        // Authenticate first
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        loginPage.loginAs("owner@rentportal.com", "owner123");

        OwnerTenantsPage tenantsPage = new OwnerTenantsPage(driver);
        assertTrue(tenantsPage.isDisplayed());

        // Perform Sign Out
        tenantsPage.clickSignOut();

        assertTrue(waitForUrlContains("logout"), "URL should contain logout parameter after signing out");
        assertTrue(loginPage.isSuccessAlertDisplayed(), "Success alert should be displayed after logout");
        assertTrue(loginPage.getSuccessMessage().contains("signed out"),
                "Success message should confirm sign out");
    }
}
