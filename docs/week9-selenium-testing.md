# Week 9: Selenium Test Design and Local Execution

This document details the test design, automation framework, Page Object Model (POM) architecture, local execution process, test reporting, and failure screenshot mechanism for the **Rent Payment Reminder Portal**.

---

## 1. Executive Summary & Objective

The primary objective of this testing initiative is to design and automate an end-to-end regression test suite using **Selenium WebDriver (4.20.0)** and **JUnit 5**, integrated into the existing **Spring Boot (3.2.5)** application ecosystem.

The test suite validates the core business workflows of the portal across multiple user roles (Property Owner and Tenant), ensuring functional reliability, form validation integrity, financial calculation correctness (deposit balances and overdue deductions), and official receipt document layout.

### Summary Metrics
| Metric | Value |
| :--- | :--- |
| **Testing Framework** | Selenium WebDriver 4.20.0 + JUnit 5 |
| **Application Runtime** | Spring Boot 3.2.5 (Embedded Tomcat) |
| **Browser Engine** | Headless Google Chrome (W3C Compliant) |
| **Design Pattern** | Page Object Model (POM) |
| **Total Test Scenarios** | 10 Scenarios |
| **User Journeys Covered** | 5 Critical Journeys (100% of portal capabilities) |
| **Test Pass Rate** | **100.0% (10 Passed, 0 Failed, 0 Skipped)** |
| **Local Execution Time** | ~31.4 seconds |
| **Failure Screenshot Mechanism** | JUnit 5 `AfterTestExecutionCallback` + `TestWatcher` |
| **Local Reports** | Surefire XML/HTML + Custom Interactive Report (`target/selenium-reports/index.html`) |

---

## 2. Identification of 5 Critical User Journeys

Five end-to-end user journeys were identified to cover the complete rental lifecycle from owner onboarding to payment receipt generation:

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                      CRITICAL USER JOURNEY WORKFLOW                    │
 └────────────────────────────────────────────────────────────────────────┘
   [Journey 1: Owner Auth]        ───>  Validates owner credentials & role routing
            │
            ▼
   [Journey 2: Tenant Signup]     ───>  Tenant creates portal self-service account
            │
            ▼
   [Journey 3: Tenant Onboarding] ───>  Owner adds tenant & dispatches pending invite
            │
            ▼
   [Journey 4: Lease Activation]  ───>  Tenant accepts invite, sets deposit & dates
            │
            ▼
   [Journey 5: Payment & Receipt] ───>  Records Paid & Overdue rent; generates receipt
```

### Journey 1: Owner Authentication, Role-based Routing & Security Control
- **Actor:** Property Owner
- **Scope:** Validates access control at `/login`.
- **Scenarios:**
  1. *Negative Validation:* Rejection of invalid credentials (`unregistered@rentportal.com` / `WrongPass`) with `alert-error` banner.
  2. *Positive Authentication:* Sign-in with default seeded credentials (`owner@rentportal.com` / `owner123`), verifying role-based redirection to the Tenants Directory (`/tenants`).
  3. *Session Termination:* Logout flow via `/logout` POST, confirming session invalidation and redirection to `/login?logout` with success message.

### Journey 2: Tenant Self-Service Registration & Portal Dashboard Access
- **Actor:** Tenant
- **Scope:** Self-registration at `/portal/register` and subsequent tenant sign-in.
- **Scenarios:**
  1. *Form Validation:* Submitting mismatched passwords triggers an inline error alert: `"Passwords do not match."`
  2. *Account Creation:* Registering with valid details (Name, unique email, phone, password), verifying automatic redirection to `/login` with success banner.
  3. *Tenant Dashboard Login:* Tenant signs into `/portal/dashboard`, verifying personalized welcome greeting (`"Welcome, <Name>"`) and initial empty state (`"No invitations yet"`).

### Journey 3: Owner Tenant Onboarding & Automated Invitation Generation
- **Actor:** Property Owner
- **Scope:** Adding a new tenant unit at `/tenants/new` and checking automated portal invitation dispatch.
- **Scenarios:**
  1. *Tenant Creation:* Owner navigates to `Add New Tenant` form and submits full name, apartment number (e.g. `302`), phone, and contact email.
  2. *Directory Verification:* Confirms redirection to `/tenants`, asserts presence in the Active Tenants table with apartment badge (`APT 302`), email, and status `"⏳ Pending Approval"`.

### Journey 4: Tenant Invitation Review & Acceptance (Lease & Deposit Configuration)
- **Actors:** Tenant & Property Owner
- **Scope:** Acceptance of the rental offer, establishing the security deposit and lease duration.
- **Scenarios:**
  1. *Invitation Detection:* Tenant logs into dashboard and receives pending notification card for their apartment.
  2. *Lease Approval Form:* Tenant opens the inline approval form, specifies security deposit (₹25,000), rental start date, and end date.
  3. *Lease Activation:* Confirms approval flash message, presence of the Active Lease card (`"Active"` badge), and 100% Deposit Balance bar.
  4. *Owner Synchronization:* Owner logs into `/tenants` to verify the tenant status updated to `"✓ Accepted"` with deposit amount and lease period displayed.

### Journey 5: Rent Payment Recording, Overdue Deposit Deduction & Official Receipt Verification
- **Actors:** Property Owner & Tenant
- **Scope:** Recording monthly rent payments at `/payments/new`, adjusting deposit for overdue rent, and generating formal printable receipts.
- **Scenarios:**
  1. *Paid Rent Entry:* Owner selects tenant, inputs amount (₹14,500), month, year, and status `"Paid"`.
  2. *Payment Summary Audit:* Verifies entry in `/payments/summary` table.
  3. *Official Printable Receipt:* Navigates to `/payments/receipt/{id}`, verifies receipt number format (`REC-XXXXX`), status stamp (`"✓ PAID IN FULL"`), line items, and dual signature blocks.
  4. *Overdue Deduction:* Owner records an `"Overdue"` rent payment (₹7,000). Asserts receipt displays `"⚠ OVERDUE (DEPOSIT DEDUCTED)"` and deduction notice.

---

## 3. Test Plan & Architecture

### 3.1 Framework Architecture
The test suite utilizes a decoupled architecture based on the **Page Object Model (POM)**:

```
src/test/java/com/rentreminder/app/selenium/
├── base/
│   ├── BaseSeleniumTest.java          # SpringBootTest runner, WebDriver lifecycle, explicit waits
│   └── ScreenshotWatcher.java         # JUnit 5 extension for failure screenshots & report collection
├── pages/
│   ├── LoginPage.java                 # Login & sign-out locators and actions
│   ├── TenantRegistrationPage.java    # Registration form locators and actions
│   ├── OwnerTenantsPage.java          # Owner directory and tabs
│   ├── AddTenantPage.java             # Add tenant form
│   ├── TenantDashboardPage.java       # Tenant dashboard, invitation forms & lease cards
│   ├── PaymentFormPage.java           # Record payment form
│   ├── PaymentSummaryPage.java        # Payment ledger table
│   └── ReceiptPage.java               # Formal printable receipt layout
├── journeys/
│   ├── Journey1_OwnerAuthenticationTest.java
│   ├── Journey2_TenantRegistrationTest.java
│   ├── Journey3_OwnerTenantOnboardingTest.java
│   ├── Journey4_TenantInvitationLeaseTest.java
│   ├── Journey5_PaymentReceiptFlowTest.java
│   └── FailureScreenshotMechanismTest.java
└── report/
    ├── TestResultRecord.java          # Data transfer object for execution outcome
    └── SeleniumReportGenerator.java   # Generates target/selenium-reports/index.html
```

### 3.2 Spring Boot Embedded Integration
Rather than requiring a pre-deployed Tomcat server, all tests extend `BaseSeleniumTest`:
- Annotated with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`.
- Injects `@LocalServerPort protected int port;`.
- Automatically initializes embedded Tomcat, Spring Security filter chains, and the SQLite persistence layer.
- Tests can run on any machine or CI agent with a single `mvn test` invocation.

### 3.3 WebDriver Lifecycle & Headless Chrome Configuration
`BaseSeleniumTest` initializes a dedicated headless Chrome instance per test:
- Arguments configured: `--headless=new`, `--no-sandbox`, `--disable-dev-shm-usage`, `--disable-gpu`, `--window-size=1920,1080`, `--remote-allow-origins=*`.
- Allows toggling visible mode locally via `-Dheadless=false`.
- Automatic fallback to Microsoft Edge WebDriver if Chrome is absent.
- Standard implicit wait: 3 seconds; standard explicit wait (`WebDriverWait`): 10 seconds.

---

## 4. Failure Screenshot Mechanism

### 4.1 Architecture & Implementation
A reliable screenshot mechanism requires capturing the browser viewport **before** test teardown (`@AfterEach`) closes the browser.

`ScreenshotWatcher` implements two JUnit 5 interfaces:
1. **`AfterTestExecutionCallback`**:
   - Executes immediately after each test method completes.
   - Checks `context.getExecutionException().isPresent()`.
   - If an exception occurred, captures a full 1080p screenshot via `((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)` while the browser is still displaying the error state.
   - Saves the image to `target/screenshots/<Class>_<Method>_<Timestamp>.png`.
2. **`TestWatcher`**:
   - Records test execution metadata (`className`, `methodName`, `journey`, `status`, `durationMs`, `errorMessage`, `screenshotPath`).
   - Forwards results to `SeleniumReportGenerator`.

### 4.2 Screenshot Storage & Naming Standard
- **Directory:** `target/screenshots/`
- **Naming Pattern:** `<TestClassName>_<TestMethodName>_yyyyMMdd_HHmmss_SSS.png`
- **Format:** Uncompressed, full-viewport PNG.

### 4.3 Practical Proof of Screenshot Utility
During initial test suite execution, the failure screenshot mechanism immediately revealed subtle UI issues that standard stack traces could not explain:
1. **Case-Sensitive CSS Text-Transform:** An assertion checked for `"Apt 302"`. Viewing the captured screenshot showed the badge rendered as `"APT 302"` due to `.badge { text-transform: uppercase; }`.
2. **HTML5 Native DatePicker Concatenation:** An assertion failed when submitting a date. The captured screenshot revealed the date input held `20-02-61001` because `.sendKeys()` appended to the pre-filled date. Switching to `JavascriptExecutor` resolved this across all locales.

---

## 5. Local Test Execution Guide

### 5.1 Prerequisites
- **Java Development Kit:** JDK 17 or JDK 21
- **Apache Maven:** 3.8+
- **Browser:** Google Chrome (or Microsoft Edge)

### 5.2 Command-Line Execution

```bash
# Execute the complete Selenium test suite
mvn test

# Execute a specific user journey
mvn test -Dtest=Journey1_OwnerAuthenticationTest
mvn test -Dtest=Journey2_TenantRegistrationTest
mvn test -Dtest=Journey3_OwnerTenantOnboardingTest
mvn test -Dtest=Journey4_TenantInvitationLeaseTest
mvn test -Dtest=Journey5_PaymentReceiptFlowTest

# Execute with visible browser window (non-headless)
mvn test -Dheadless=false

# Generate standard Maven Surefire HTML report
mvn surefire-report:report-only
```

---

## 6. Test Execution Results & Reports

### 6.1 Execution Results Matrix
| # | Test Class | Scenario Name | User Journey | Result | Duration |
| :-: | :--- | :--- | :--- | :-: | :-: |
| 1 | `Journey1_OwnerAuthenticationTest` | `testOwnerLoginInvalidCredentials` | Journey 1: Owner Authentication | **PASSED** | 1.8s |
| 2 | `Journey1_OwnerAuthenticationTest` | `testOwnerLoginValidCredentialsAndRedirection` | Journey 1: Owner Authentication | **PASSED** | 1.5s |
| 3 | `Journey1_OwnerAuthenticationTest` | `testOwnerSignOutFlow` | Journey 1: Owner Authentication | **PASSED** | 1.6s |
| 4 | `Journey2_TenantRegistrationTest` | `testTenantRegistrationPasswordMismatch` | Journey 2: Tenant Registration | **PASSED** | 1.4s |
| 5 | `Journey2_TenantRegistrationTest` | `testTenantRegistrationSuccessAndDashboardLogin` | Journey 2: Tenant Registration | **PASSED** | 2.1s |
| 6 | `Journey3_OwnerTenantOnboardingTest` | `testOwnerAddTenantAndAutomatedInvitationCreation` | Journey 3: Tenant Onboarding | **PASSED** | 1.8s |
| 7 | `Journey4_TenantInvitationLeaseTest` | `testTenantInvitationReviewAcceptanceAndDepositSetup` | Journey 4: Lease & Deposit Setup | **PASSED** | 3.6s |
| 8 | `Journey5_PaymentReceiptFlowTest` | `testRecordPaidPaymentAndVerifyReceipt` | Journey 5: Payment & Receipt | **PASSED** | 2.5s |
| 9 | `Journey5_PaymentReceiptFlowTest` | `testRecordOverduePaymentAndVerifyDeductionReceipt` | Journey 5: Payment & Receipt | **PASSED** | 2.4s |
| 10 | `FailureScreenshotMechanismTest` | `testScreenshotCaptureMechanismProducesValidFile` | Mechanism Verification | **PASSED** | 1.2s |

**Final Outcome:**
- **Total Tests Run:** 10
- **Passed:** 10
- **Failures:** 0
- **Errors:** 0
- **Pass Rate:** **100%**
- **Maven Build Status:** `BUILD SUCCESS`

### 6.2 Report Artifacts Generated
1. **Interactive HTML Suite Report:**
   - Location: `target/selenium-reports/index.html`
   - Contains KPI metric cards, journey progress chips, tabular scenario breakdown, and links to failure screenshots.
2. **Maven Surefire Report:**
   - Location: `target/site/surefire-report.html`
   - XML Summaries: `target/surefire-reports/TEST-*.xml`
3. **Failure Screenshots Archive:**
   - Location: `target/screenshots/*.png`

---

## 7. Deliverable Checklist

- [x] Identified 5 critical user journeys covering owner auth, tenant signup, onboarding, lease approval, and payments/receipts.
- [x] Designed robust Page Object Model (POM) classes for all application views.
- [x] Implemented Selenium WebDriver tests with comprehensive assertions and unique test data per run.
- [x] Built an automated failure screenshot mechanism using JUnit 5 `AfterTestExecutionCallback`.
- [x] Verified full local execution through Maven (`mvn test`), achieving a 100% pass rate.
- [x] Generated both Maven Surefire and custom interactive HTML reports.
- [x] Documented test plan, architecture, execution steps, and results in `docs/week9-selenium-testing.md`.
