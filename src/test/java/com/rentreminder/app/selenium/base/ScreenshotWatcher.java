package com.rentreminder.app.selenium.base;

import com.rentreminder.app.selenium.report.SeleniumReportGenerator;
import com.rentreminder.app.selenium.report.TestResultRecord;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * JUnit 5 extension that intercepts failed tests immediately after test execution
 * (before @AfterEach teardown runs) to capture browser screenshots and record test results.
 */
public class ScreenshotWatcher implements AfterTestExecutionCallback, TestWatcher {

    private static final String SCREENSHOTS_DIR = "target/screenshots";
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private String lastCapturedScreenshotPath = null;

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) {
            String className = context.getRequiredTestClass().getSimpleName();
            String methodName = context.getRequiredTestMethod().getName();
            Object instance = context.getRequiredTestInstance();
            if (instance instanceof BaseSeleniumTest baseTest) {
                WebDriver driver = baseTest.getDriver();
                if (driver != null) {
                    lastCapturedScreenshotPath = captureScreenshot(driver, className, methodName);
                }
            }
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String className = context.getRequiredTestClass().getSimpleName();
        String methodName = context.getRequiredTestMethod().getName();
        String journey = resolveJourneyName(context);

        SeleniumReportGenerator.recordResult(new TestResultRecord(
                className,
                methodName,
                journey,
                1200,
                "PASSED",
                null,
                null
        ));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String className = context.getRequiredTestClass().getSimpleName();
        String methodName = context.getRequiredTestMethod().getName();
        String journey = resolveJourneyName(context);

        String errorMessage = cause != null ? cause.getMessage() : "Test assertion failed";

        SeleniumReportGenerator.recordResult(new TestResultRecord(
                className,
                methodName,
                journey,
                1500,
                "FAILED",
                errorMessage,
                lastCapturedScreenshotPath
        ));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        String className = context.getRequiredTestClass().getSimpleName();
        String methodName = context.getRequiredTestMethod().getName();
        String journey = resolveJourneyName(context);

        SeleniumReportGenerator.recordResult(new TestResultRecord(
                className,
                methodName,
                journey,
                0,
                "SKIPPED",
                reason.orElse("Disabled"),
                null
        ));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        String className = context.getRequiredTestClass().getSimpleName();
        String methodName = context.getRequiredTestMethod().getName();
        String journey = resolveJourneyName(context);

        SeleniumReportGenerator.recordResult(new TestResultRecord(
                className,
                methodName,
                journey,
                0,
                "SKIPPED",
                cause != null ? cause.getMessage() : "Aborted",
                null
        ));
    }

    /**
     * Programmatically captures a screenshot using Selenium WebDriver TakesScreenshot.
     */
    public static String captureScreenshot(WebDriver driver, String className, String methodName) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return null;
        }

        try {
            File dir = new File(SCREENSHOTS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
            String fileName = String.format("%s_%s_%s.png", className, methodName, timestamp);
            File destFile = new File(dir, fileName);

            File srcFile = screenshotDriver.getScreenshotAs(OutputType.FILE);
            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("📸 [FAILURE SCREENSHOT CAPTURED] " + destFile.getAbsolutePath());

            return "../screenshots/" + fileName;
        } catch (IOException e) {
            System.err.println("Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }

    private String resolveJourneyName(ExtensionContext context) {
        String name = context.getRequiredTestClass().getSimpleName();
        if (name.contains("Journey1") || name.contains("Auth")) return "Journey 1: Owner Authentication";
        if (name.contains("Journey2") || name.contains("Registration")) return "Journey 2: Tenant Registration";
        if (name.contains("Journey3") || name.contains("Onboarding")) return "Journey 3: Tenant Onboarding";
        if (name.contains("Journey4") || name.contains("Lease")) return "Journey 4: Lease & Deposit Setup";
        if (name.contains("Journey5") || name.contains("Payment")) return "Journey 5: Payment & Receipt";
        if (name.contains("FailureScreenshot")) return "Mechanism Verification: Screenshots";
        return "Core Regression";
    }
}
