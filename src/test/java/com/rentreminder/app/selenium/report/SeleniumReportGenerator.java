package com.rentreminder.app.selenium.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates an executive HTML test execution report for the Selenium test suite.
 * Saved to target/selenium-reports/index.html
 */
public class SeleniumReportGenerator {

    private static final List<TestResultRecord> records = Collections.synchronizedList(new ArrayList<>());
    private static final Object lock = new Object();

    public static void recordResult(TestResultRecord record) {
        records.add(record);
        generateHtmlReport();
    }

    public static List<TestResultRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public static synchronized void generateHtmlReport() {
        synchronized (lock) {
            File reportDir = new File("target/selenium-reports");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            File reportFile = new File(reportDir, "index.html");

            long totalTests = records.size();
            long passedTests = records.stream().filter(r -> "PASSED".equalsIgnoreCase(r.getStatus())).count();
            long failedTests = records.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count();
            long skippedTests = records.stream().filter(r -> "SKIPPED".equalsIgnoreCase(r.getStatus())).count();
            long totalDurationMs = records.stream().mapToLong(TestResultRecord::getDurationMs).sum();
            double passRate = totalTests > 0 ? ((double) passedTests / totalTests) * 100.0 : 0.0;

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Selenium Test Execution Report — Rent Reminder Portal</title>\n");
            html.append("    <style>\n");
            html.append("        :root {\n");
            html.append("            --bg: #0f172a;\n");
            html.append("            --card-bg: #1e293b;\n");
            html.append("            --card-border: #334155;\n");
            html.append("            --text-main: #f8fafc;\n");
            html.append("            --text-muted: #94a3b8;\n");
            html.append("            --accent: #3b82f6;\n");
            html.append("            --success: #10b981;\n");
            html.append("            --danger: #ef4444;\n");
            html.append("            --warning: #f59e0b;\n");
            html.append("        }\n");
            html.append("        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n");
            html.append("        body { background-color: var(--bg); color: var(--text-main); padding: 2rem 1rem; line-height: 1.5; }\n");
            html.append("        .container { max-width: 1200px; margin: 0 auto; }\n");
            html.append("        .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; border-bottom: 1px solid var(--card-border); padding-bottom: 1.5rem; }\n");
            html.append("        .title h1 { font-size: 1.85rem; font-weight: 700; color: #fff; display: flex; align-items: center; gap: 0.5rem; }\n");
            html.append("        .title p { color: var(--text-muted); font-size: 0.95rem; margin-top: 0.35rem; }\n");
            html.append("        .meta-badge { background: #1e3a8a; color: #93c5fd; padding: 0.35rem 0.8rem; border-radius: 9999px; font-size: 0.8rem; font-weight: 600; }\n");
            html.append("        .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 2rem; }\n");
            html.append("        .kpi-card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 1.25rem; text-align: center; }\n");
            html.append("        .kpi-card .value { font-size: 2rem; font-weight: 700; margin-top: 0.25rem; }\n");
            html.append("        .kpi-card .label { font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }\n");
            html.append("        .kpi-card.passed .value { color: var(--success); }\n");
            html.append("        .kpi-card.failed .value { color: var(--danger); }\n");
            html.append("        .kpi-card.rate .value { color: var(--accent); }\n");
            html.append("        .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 1.5rem; margin-bottom: 2rem; }\n");
            html.append("        .card-header { margin-bottom: 1rem; font-size: 1.2rem; font-weight: 600; display: flex; justify-content: space-between; align-items: center; }\n");
            html.append("        table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }\n");
            html.append("        th { background: #0f172a; color: var(--text-muted); text-align: left; padding: 0.75rem 1rem; font-weight: 600; border-bottom: 2px solid var(--card-border); }\n");
            html.append("        td { padding: 0.85rem 1rem; border-bottom: 1px solid var(--card-border); vertical-align: middle; }\n");
            html.append("        tr:hover { background: rgba(255,255,255,0.02); }\n");
            html.append("        .badge { display: inline-block; padding: 0.25rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; }\n");
            html.append("        .badge-passed { background: rgba(16,185,129,0.15); color: var(--success); border: 1px solid rgba(16,185,129,0.3); }\n");
            html.append("        .badge-failed { background: rgba(239,68,68,0.15); color: var(--danger); border: 1px solid rgba(239,68,68,0.3); }\n");
            html.append("        .badge-skipped { background: rgba(245,158,11,0.15); color: var(--warning); border: 1px solid rgba(245,158,11,0.3); }\n");
            html.append("        .journey-tag { background: #334155; color: #cbd5e1; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; }\n");
            html.append("        .error-box { margin-top: 0.5rem; padding: 0.5rem; background: #450a0a; border: 1px solid #7f1d1d; border-radius: 6px; color: #fca5a5; font-family: monospace; font-size: 0.8rem; overflow-x: auto; }\n");
            html.append("        .screenshot-link { display: inline-flex; align-items: center; gap: 0.3rem; color: #60a5fa; text-decoration: none; font-size: 0.8rem; font-weight: 600; }\n");
            html.append("        .screenshot-link:hover { text-decoration: underline; }\n");
            html.append("        .journeys-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1rem; margin-bottom: 2rem; }\n");
            html.append("        .journey-item { background: #131d2e; border: 1px solid #23354d; border-radius: 8px; padding: 1rem; }\n");
            html.append("        .journey-item h4 { font-size: 0.95rem; margin-bottom: 0.25rem; color: #93c5fd; }\n");
            html.append("        .journey-item p { font-size: 0.82rem; color: var(--text-muted); }\n");
            html.append("        .footer { text-align: center; color: var(--text-muted); font-size: 0.8rem; margin-top: 2rem; }\n");
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("<div class=\"container\">\n");
            html.append("    <div class=\"header\">\n");
            html.append("        <div class=\"title\">\n");
            html.append("            <h1>🏠 RentPortal — Selenium WebDriver Execution Report</h1>\n");
            html.append("            <p>Automated End-to-End Regression Suite across Critical User Journeys</p>\n");
            html.append("        </div>\n");
            html.append("        <div>\n");
            html.append("            <span class=\"meta-badge\">Spring Boot 3.2.5 + Selenium 4.20.0</span>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");

            // KPI Cards
            html.append("    <div class=\"kpi-grid\">\n");
            html.append("        <div class=\"kpi-card\">\n");
            html.append("            <div class=\"label\">Total Scenarios</div>\n");
            html.append("            <div class=\"value\">").append(totalTests).append("</div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"kpi-card passed\">\n");
            html.append("            <div class=\"label\">Passed</div>\n");
            html.append("            <div class=\"value\">").append(passedTests).append("</div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"kpi-card failed\">\n");
            html.append("            <div class=\"label\">Failed</div>\n");
            html.append("            <div class=\"value\">").append(failedTests).append("</div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"kpi-card rate\">\n");
            html.append("            <div class=\"label\">Pass Rate</div>\n");
            html.append("            <div class=\"value\">").append(String.format("%.1f%%", passRate)).append("</div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"kpi-card\">\n");
            html.append("            <div class=\"label\">Execution Duration</div>\n");
            html.append("            <div class=\"value\">").append(String.format("%.2fs", totalDurationMs / 1000.0)).append("</div>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");

            // Critical Journeys Architecture
            html.append("    <div class=\"card\">\n");
            html.append("        <div class=\"card-header\">🎯 5 Critical User Journeys Tested</div>\n");
            html.append("        <div class=\"journeys-grid\">\n");
            html.append("            <div class=\"journey-item\">\n");
            html.append("                <h4>Journey 1: Owner Authentication &amp; Security</h4>\n");
            html.append("                <p>Covers valid login, invalid credential rejection, role-based redirection to /tenants, and sign-out invalidation.</p>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"journey-item\">\n");
            html.append("                <h4>Journey 2: Tenant Self-Service Registration</h4>\n");
            html.append("                <p>Validates registration form integrity, password mismatch validation, account creation, and landing on tenant dashboard.</p>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"journey-item\">\n");
            html.append("                <h4>Journey 3: Owner Tenant Onboarding</h4>\n");
            html.append("                <p>Verifies property owner adding tenants with apartment assignment and automated pending invitation dispatch.</p>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"journey-item\">\n");
            html.append("                <h4>Journey 4: Tenant Lease &amp; Deposit Activation</h4>\n");
            html.append("                <p>Tests tenant approving pending invitation, setting security deposit amount, date ranges, and verifying active status in directory.</p>\n");
            html.append("            </div>\n");
            html.append("            <div class=\"journey-item\">\n");
            html.append("                <h4>Journey 5: Payment &amp; Printable Receipt Flow</h4>\n");
            html.append("                <p>Records monthly payments, tests overdue deduction from deposit, and audits official receipt layout with status stamp and signature lines.</p>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");

            // Test Results Table
            html.append("    <div class=\"card\">\n");
            html.append("        <div class=\"card-header\">📋 Scenario Execution Details</div>\n");
            html.append("        <table>\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>User Journey</th>\n");
            html.append("                    <th>Test Class / Scenario</th>\n");
            html.append("                    <th>Duration</th>\n");
            html.append("                    <th>Status</th>\n");
            html.append("                    <th>Artifacts / Details</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            for (TestResultRecord r : records) {
                String badgeClass = "badge-passed";
                if ("FAILED".equalsIgnoreCase(r.getStatus())) {
                    badgeClass = "badge-failed";
                } else if ("SKIPPED".equalsIgnoreCase(r.getStatus())) {
                    badgeClass = "badge-skipped";
                }

                html.append("                <tr>\n");
                html.append("                    <td><span class=\"journey-tag\">").append(escapeHtml(r.getJourneyName())).append("</span></td>\n");
                html.append("                    <td>\n");
                html.append("                        <strong>").append(escapeHtml(r.getMethodName())).append("</strong><br>\n");
                html.append("                        <span style=\"font-size:0.75rem; color:var(--text-muted);\">").append(escapeHtml(r.getClassName())).append("</span>\n");
                html.append("                    </td>\n");
                html.append("                    <td>").append(r.getDurationMs()).append(" ms</td>\n");
                html.append("                    <td><span class=\"badge ").append(badgeClass).append("\">").append(r.getStatus()).append("</span></td>\n");
                html.append("                    <td>\n");
                if (r.getScreenshotPath() != null && !r.getScreenshotPath().isBlank()) {
                    html.append("                        <a class=\"screenshot-link\" href=\"").append(escapeHtml(r.getScreenshotPath())).append("\" target=\"_blank\">📸 View Screenshot</a>\n");
                }
                if (r.getErrorMessage() != null && !r.getErrorMessage().isBlank()) {
                    html.append("                        <div class=\"error-box\">").append(escapeHtml(r.getErrorMessage())).append("</div>\n");
                }
                if ((r.getScreenshotPath() == null || r.getScreenshotPath().isBlank()) && (r.getErrorMessage() == null || r.getErrorMessage().isBlank())) {
                    html.append("                        <span style=\"color:var(--text-muted); font-size:0.8rem;\">Executed cleanly</span>\n");
                }
                html.append("                    </td>\n");
                html.append("                </tr>\n");
            }

            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");

            // Footer
            html.append("    <div class=\"footer\">\n");
            html.append("        Rent Payment Reminder Portal — Automated Selenium Test Suite &bull; Local Execution Verified\n");
            html.append("    </div>\n");
            html.append("</div>\n");
            html.append("</body>\n");
            html.append("</html>\n");

            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write(html.toString());
            } catch (IOException e) {
                System.err.println("Failed to write HTML report: " + e.getMessage());
            }
        }
    }

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
