package com.rentreminder.app.selenium.journeys;

import com.rentreminder.app.selenium.base.BaseSeleniumTest;
import com.rentreminder.app.selenium.base.ScreenshotWatcher;
import com.rentreminder.app.selenium.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Selenium Failure Screenshot Mechanism.
 * Verifies that the browser viewport is captured via TakesScreenshot,
 * written to target/screenshots/, and verified as a non-empty image file.
 */
public class FailureScreenshotMechanismTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Mechanism Verification: Verify screenshot capture produces a valid PNG file in target/screenshots")
    void testScreenshotCaptureMechanismProducesValidFile() {
        LoginPage loginPage = new LoginPage(driver).open(getBaseUrl());
        assertTrue(loginPage.isDisplayed());

        // Trigger screenshot capture through the watcher utility
        String relativePath = ScreenshotWatcher.captureScreenshot(driver, "FailureScreenshotMechanismTest", "demoFailureCapture");
        assertNotNull(relativePath, "Screenshot utility must return the relative path for reporting");
        assertTrue(relativePath.startsWith("../screenshots/"), "Relative path must point to ../screenshots/ folder");

        // Convert to local file path and assert existence
        String fileName = relativePath.replace("../screenshots/", "");
        File screenshotFile = new File("target/screenshots", fileName);

        assertTrue(screenshotFile.exists(), "Captured screenshot file must exist on the disk");
        assertTrue(screenshotFile.length() > 1000, "Screenshot file size must be non-empty (> 1KB)");
        assertTrue(screenshotFile.getName().endsWith(".png"), "Screenshot file must have .png extension");
    }
}
