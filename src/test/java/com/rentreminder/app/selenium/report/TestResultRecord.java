package com.rentreminder.app.selenium.report;

import java.time.LocalDateTime;

/**
 * Data model representing the execution outcome of an individual Selenium test case.
 */
public class TestResultRecord {

    private final String className;
    private final String methodName;
    private final String journeyName;
    private final long durationMs;
    private final String status; // PASSED, FAILED, SKIPPED
    private final String errorMessage;
    private final String screenshotPath;
    private final LocalDateTime timestamp;

    public TestResultRecord(String className,
                            String methodName,
                            String journeyName,
                            long durationMs,
                            String status,
                            String errorMessage,
                            String screenshotPath) {
        this.className = className;
        this.methodName = methodName;
        this.journeyName = journeyName;
        this.durationMs = durationMs;
        this.status = status;
        this.errorMessage = errorMessage;
        this.screenshotPath = screenshotPath;
        this.timestamp = LocalDateTime.now();
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getJourneyName() {
        return journeyName;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
