# Project Viva Examination Preparation Report: Rent Payment Reminder Portal

**Repository:** `Backwardbus03/Jenkins-mini-project`  
**Academic Context:** BE CMPN B (Batch-1, Final Year Computer Engineering)  
**System Type:** Full-Stack Enterprise Web Application with Automated CI/CD & Configuration Management  
**Primary Stack:** Java 17, Spring Boot 3.2.5, Spring Security 6, Thymeleaf, SQLite/MySQL, Selenium WebDriver 4, Jenkins Declarative Pipeline, Docker, Ansible  
**Generated Date:** March 2026  

---

## A. Project Overview

### Problem Statement & Motivation
Managing residential rental properties and PG (Paying Guest) accommodations often relies on informal, fragmented channels such as physical notebooks, paper ledgers, or messaging apps (e.g., WhatsApp). This manual approach causes:
1. **Financial Discrepancies & Untracked Arrears:** Lack of systematic recording for partial payments, overdue rent, and security deposit deductions.
2. **Missing Audit Trails & Disputed Receipts:** Renters lack verifiable, immutable proof of rent payments, leading to disputes over arrears and deposit refunds upon move-out.
3. **Decoupled Tenant-Landlord Lifecycle:** Traditional small-scale rental apps either treat tenants as read-only contacts or require over-engineered enterprise ERPs without direct lease agreement confirmation.
4. **DevOps Lifecycle Deficit in Academic Projects:** Most academic software projects stop at local application development, ignoring continuous testing, automated build pipelines, containerized lifecycle management, and zero-downtime server provisioning.

The **Rent Payment Reminder Portal** addresses this by coupling an end-to-end Property Management System (PMS) with an enterprise-grade CI/CD and Configuration Management pipeline.

### Objectives & Intended Users
- **Primary Objectives:**
  - Provide Property Owners with a central directory to manage active/expired leases, record monthly rent collections, and audit rent defaults.
  - Provide Tenants with a self-service dashboard to review onboarding invitations, confirm lease duration, initialize security deposits, view payment histories, and download printable rent receipts.
  - Automate financial governance by deducting overdue rental arrears directly from the tenant's security deposit balance.
  - Implement a rigorous DevOps pipeline enforcing zero-defect deployments through automated Selenium UI regression gates in Jenkins, containerized multi-stage Docker builds, and idempotent Ansible provisioning.
- **Intended Users:**
  - **Property Owners / Landlords (`ROLE_OWNER`):** Manage property units, onboard tenants, track financial cash flows, record monthly rent statuses, and inspect receipts.
  - **Tenants / Renters (`ROLE_TENANT`):** Register self-service portal accounts, approve lease terms, track remaining deposit balances, and download PDF receipts.

### Main Features & Current Implementation Status
| Feature | Implementation Status | Ground Truth in Codebase |
| :--- | :--- | :--- |
| **Dual-Role Authentication & Smart Routing** | **Implemented** | `SecurityConfig.java`, `CustomUserDetailsService.java`, `HomeController.java` |
| **Owner Tenant Directory & Tabbed Archival** | **Implemented** | `TenantController.java`, `tenants/list.html` (Active vs Inactive/Expired tabs) |
| **Automated Invitation & Account Auto-Linking**| **Implemented** | `TenantInvitationService.java`, `TenantAccountService.java` |
| **Self-Service Tenant Portal & Lease Approval** | **Implemented** | `TenantPortalController.java`, `portal/dashboard.html` |
| **Payment Tracking & Overdue Deposit Deduction**| **Implemented** | `PaymentService.java` (`deductFromDeposit`), `payments/form.html` |
| **Duplicate Payment Prevention per Month/Year** | **Implemented** | `PaymentRepository.findByTenantAndMonthAndYear`, `PaymentService.java` |
| **Printable Official Rent Receipts with Stamps** | **Implemented** | `payments/receipt.html`, `PaymentController.java`, `TenantPortalController.java` |
| **Automated Headless Selenium Regression Suite**| **Implemented** | 10 Scenarios across 5 Journeys (`src/test/java/.../selenium/journeys`) |
| **Failure Screenshot & Interactive HTML Report**| **Implemented** | `ScreenshotWatcher.java`, `SeleniumReportGenerator.java` |
| **Jenkins CI/CD Declarative Quality Gate** | **Implemented** | `Jenkinsfile` (Checkout $\rightarrow$ Compile $\rightarrow$ Test $\rightarrow$ Package $\rightarrow$ Docker $\rightarrow$ Deploy) |
| **Multi-Stage & Local Docker Containerization** | **Implemented** | `Dockerfile`, `Dockerfile.local`, named volume `rent_data` |
| **Ansible Zero-Downtime Symlink Provisioning** | **Implemented** | `ansible/site.yml`, `roles/prerequisites`, `roles/portal_release`, `rollback.yml` |
| **Automated SMS / Email Notification Dispatch** | **Planned / Unverified** | Mentioned in SRS design concepts; currently represented via in-app alerts and invitation linkage. |

### What Makes the Project Useful
Unlike typical CRUD demonstrations, this project bridges application business logic with production DevOps engineering. It prevents bad code from ever reaching production by halting the Jenkins pipeline if any browser UI test fails, protects SQLite persistent state inside Docker named volumes, and enables sub-second system rollbacks using Linux symlink pointer switches managed by Ansible.

### The 30-Second Elevator Pitch (Memorize for Viva Opening)
> *"The Rent Payment Reminder Portal is a production-hardened web application and automated DevOps pipeline built with Spring Boot 3, Thymeleaf, and Spring Security. It digitizes the landlord-tenant lifecycle by providing dual-role dashboards, automated lease onboarding, and financial reconciliation—including automatic security deposit deductions when rent is marked overdue. Technically, what distinguishes this project is its automated quality engineering: every commit triggers a Jenkins declarative pipeline that executes a 10-scenario Selenium UI regression suite in headless Chrome. If a regression occurs, the build halts immediately with failure screenshots. Upon passing, the pipeline builds an unprivileged multi-stage Docker container with persistent volumes or deploys via Ansible using an atomic symlink release architecture with instant rollback capabilities."*

---

## B. Technology Stack

| Technology / Library | Role in Project | Why Suitable for this Use Case | Location in Codebase |
| :--- | :--- | :--- | :--- |
| **Java 17 (LTS)** | Core Programming Language | High-performance enterprise runtime, strongly typed, native records, pattern matching, sealed types. | `pom.xml` (`<java.version>17</java.version>`), all `src/**/*.java` |
| **Spring Boot 3.2.5** | Backend Application Framework | Rapid enterprise bootstrapping, convention-over-configuration, embedded Tomcat, unified dependency management. | `pom.xml`, `RentReminderApplication.java` |
| **Spring Security 6** | Authentication & Authorization | Robust filter-chain security, BCrypt password hashing, role-based access control (`ROLE_OWNER`, `ROLE_TENANT`), CSRF protection. | `config/SecurityConfig.java`, `config/CustomUserDetailsService.java` |
| **Spring Data JPA & Hibernate 6** | Object-Relational Mapping (ORM) | Declarative repository pattern (`JpaRepository`), automated schema migrations (`ddl-auto=update`), dialect abstraction. | `pom.xml`, `repository/*`, `model/*` |
| **SQLite (Xerial JDBC 3.x)** | Primary Application Database | Zero-configuration, serverless, file-backed relational store (`rentdb.sqlite`). Ideal for embedded labs, small VMs, and low-footprint containers. | `pom.xml`, `application.properties`, `Dockerfile` |
| **Hibernate Community Dialects** | SQLite Dialect Extension | Provides `SQLiteDialect` for Hibernate 6 in Spring Boot 3.2+ where SQLite is not in Hibernate core. | `pom.xml` (`hibernate-community-dialects`), `application.properties` |
| **Thymeleaf 3 & Extras** | Server-Side Template Engine | Natural HTML templating without complex Node.js build steps, dynamic model rendering, Spring Security tag integration. | `src/main/resources/templates/**/*.html` |
| **Jakarta Bean Validation** | Declarative Data Validation | Enforces business rules (`@NotBlank`, `@Min`, `@NotNull`) at the controller and persistence layers before SQL execution. | `model/Tenant.java`, `model/Payment.java`, `controller/*` |
| **Selenium WebDriver 4.20.0** | Browser Automation Testing | W3C-standardized browser control, headless Chrome execution, end-to-end multi-role journey testing. | `pom.xml`, `src/test/java/.../selenium/**/*` |
| **JUnit 5 (Jupiter)** | Unit & Integration Test Runner | Modern test framework supporting `@ExtendWith`, `TestWatcher`, parameterized executions, lifecycle callbacks. | `src/test/java/...`, `base/ScreenshotWatcher.java` |
| **Apache Maven 3.9** | Build & Dependency Tool | Declarative build lifecycle (`compile`, `test`, `package`), plugin execution (`maven-surefire-plugin`). | `pom.xml`, `Jenkinsfile`, `Dockerfile` |
| **Jenkins 2.x** | CI/CD Automation Server | Declarative pipeline-as-code (`Jenkinsfile`), automated quality gates, test result archiving, container lifecycle orchestration. | `Jenkinsfile`, `docs/week7*`, `docs/week10*`, `docs/week12*` |
| **Docker (Jammy / Temurin 17)** | Containerization Engine | Immutable deployment packaging, multi-stage size reduction, unprivileged user execution, named volume persistence. | `Dockerfile`, `Dockerfile.local`, `.dockerignore` |
| **Ansible 2.14+** | Configuration Management | Agentless, declarative YAML automation over SSH, idempotent node provisioning, atomic symlink zero-downtime deployment. | `ansible/site.yml`, `ansible/roles/*`, `ansible/rollback.yml` |

---

## C. System Architecture

### Architectural Diagram (Viva Reproduction Ready)

```
 +----------------------------------------------------------------------------------------------------+
 |                                      CLIENT TIER (Web Browser)                                     |
 |  Property Owner (Browser)                         Tenant / Resident (Browser)                      |
 |  [ http://localhost:8081/tenants ]                [ http://localhost:8081/portal/dashboard ]       |
 +----------------------------------+----------------------------------+------------------------------+
                                    | HTTP Requests (Port 8081)        |
                                    v                                  v
 +----------------------------------------------------------------------------------------------------+
 |                                  PRESENTATION & SECURITY LAYER                                     |
 |  - Spring Security Filter Chain (CSRF Validation, Session Management, BCrypt Hashing)               |
 |  - CustomUserDetailsService (Unifies OwnerAccount & TenantAccount credentials)                     |
 |  - Role-Based Authentication Success Handler (Owner -> /tenants | Tenant -> /portal/dashboard)     |
 |  - Thymeleaf View Resolvers (/templates/tenants, /templates/payments, /templates/portal)          |
 +----------------------------------------------------------------------------------------------------+
                                    | Dispatches validated requests
                                    v
 +----------------------------------------------------------------------------------------------------+
 |                                      CONTROLLER LAYER (MVC)                                        |
 |   +----------------------+   +-----------------------+   +-------------------------------------+   |
 |   |   TenantController   |   |   PaymentController   |   |        TenantPortalController       |   |
 |   |   /tenants/**        |   |   /payments/**        |   |        /portal/**                   |   |
 |   +----------+-----------+   +-----------+-----------+   +------------------+------------------+   |
 +--------------|---------------------------|----------------------------------|----------------------+
                | Invokes                   | Invokes                          | Invokes
                v                           v                                  v
 +----------------------------------------------------------------------------------------------------+
 |                                  SERVICE LAYER (Business Logic)                                    |
 |   +-----------------------+  +-----------------------+  +--------------------------------------+   |
 |   |     TenantService     |  |    PaymentService     |  |        TenantInvitationService       |   |
 |   | - CRUD Operations     |  | - Duplicate Check     |  | - Pending/Approved/Rejected State    |   |
 |   | - Lease Expiry Filter |  | - Lease Validity Guard|  | - Security Deposit Deduction Engine  |   |
 |   +-----------+-----------+  +-----------+-----------+  +-------------------+------------------+   |
 |               |                          |                                  |                      |
 |               +--------------------+     | Checks & Deducts                 |                      |
 |                                    v     v                                  v                      |
 |                             +--------------------------------------------------+                   |
 |                             |        TenantAccountService / OwnerService       |                   |
 +-----------------------------+--------------------------------------------------+-------------------+
                               | Data Access Objects (Spring Data JPA)
                               v
 +----------------------------------------------------------------------------------------------------+
 |                                  PERSISTENCE LAYER (Repositories)                                  |
 |  OwnerAccountRepo | TenantAccountRepo | TenantInvitationRepo | TenantRepo | PaymentRepo            |
 +----------------------------------------------------------------------------------------------------+
                               | SQL / Dialect Queries
                               v
 +----------------------------------------------------------------------------------------------------+
 |                              DATABASE & INFRASTRUCTURE TIER                                        |
 |   +--------------------------------------------------------------------------------------------+   |
 |   | SQLite Database: /app/data/rentdb.sqlite (Mounted via Docker Volume 'rent_data')           |   |
 |   | Staging/Prod Fallback: MySQL 8.0 on TCP 3306                                               |   |
 |   +--------------------------------------------------------------------------------------------+   |
 |   | CI/CD & Deploy Engine: Jenkins Pipeline -> Docker Engine -> Ansible Symlink Manager        |   |
 +---+--------------------------------------------------------------------------------------------+---+
```

### Component Responsibilities & Communication Flow
1. **Security & Session Handling:** Every HTTP request enters Spring Security's filter chain. Unauthenticated requests are directed to `/login`. Upon authentication, `roleBasedSuccessHandler` inspects the user's granted authorities (`ROLE_OWNER` vs `ROLE_TENANT`) and dynamically redirects them to their specialized workspace.
2. **Controller Layer:** Controllers (`TenantController`, `PaymentController`, `TenantPortalController`) process HTTP GET/POST verbs, validate form binding results using `@Valid`, and populate the Thymeleaf `Model` map.
3. **Service Layer:** Houses the core business rules:
   - `PaymentService` verifies that the tenant's lease has not expired via `TenantInvitationService.isTenantLeaseExpired()` before recording payment.
   - `PaymentService` ensures no duplicate payments exist for the same month and year via `PaymentRepository.findByTenantAndMonthAndYear()`.
   - `TenantInvitationService` computes deposit deductions and auto-links newly registered tenant user accounts to pending owner invitations by email address.
4. **Data Layer:** JPA repositories extend `JpaRepository<T, ID>`, generating SQL statements at runtime through Hibernate 6 and the `SQLiteDialect`.

---

## D. End-to-End Execution Flow

### Primary Workflow: The Complete Rental Lifecycle
Tracing the journey from tenant onboarding to automated overdue deposit deduction and receipt verification:

```
[Owner Action]
1. Owner opens /tenants/new -> Submits Tenant details (Name: 'Jane Doe', Email: 'jane@example.com', Apt: '101')
   -> Handled by TenantController.saveTenant()
   -> Saves Tenant entity via TenantService.saveTenant()
   -> Invokes TenantInvitationService.createInvitation(tenant, email)
   -> Generates TenantInvitation record with STATUS_PENDING
   -> Redirects to /tenants (displayed under "Active Tenants" tab with badge 'Pending Approval')

[Tenant Action]
2. Tenant browses to /portal/register -> Submits Name, Email ('jane@example.com'), Phone, Password
   -> Handled by TenantPortalController.register()
   -> TenantAccountService.register() hashes password with BCrypt
   -> Queries TenantInvitationRepository.findByTenantEmail('jane@example.com')
   -> Automatically attaches newly created TenantAccount ID to the pending TenantInvitation!
   -> Redirects to /login

3. Tenant logs in with 'jane@example.com'
   -> CustomUserDetailsService loads account, assigns ROLE_TENANT
   -> Redirection handler sends browser to /portal/dashboard
   -> Dashboard renders "Pending Invitations" card for Apartment 101

4. Tenant clicks "Approve & Set Deposit" -> Submits Deposit (₹25,000), Start Date (2026-01-01), End Date (2027-01-01)
   -> Handled by TenantPortalController.approveInvite()
   -> TenantInvitationService.approve() sets STATUS_APPROVED, depositAmount=25000, depositBalance=25000
   -> Redirects to /portal/dashboard -> Renders active lease card with live deposit balance

[Owner Financial Action]
5. Owner opens /payments/new -> Selects 'Jane Doe', Month: 3, Year: 2026, Amount: ₹8,000, Status: 'Overdue'
   -> Handled by PaymentController.savePayment()
   -> Checks if lease is expired (isTenantLeaseExpired -> false)
   -> Checks for duplicate payment for Month 3, Year 2026 -> None exists
   -> PaymentRepository.save(payment) persists payment record
   -> Because status is 'Overdue':
      TenantInvitationService.deductFromDeposit(tenant, 8000) is triggered!
      Deposit balance updated: ₹25,000 - ₹8,000 = ₹17,000!

[Receipt Generation]
6. User clicks "View Receipt" -> GET /payments/receipt/{id}
   -> PaymentController / TenantPortalController loads Payment entity
   -> receipt.html checks status -> Renders bold red stamp: "⚠ OVERDUE (DEPOSIT DEDUCTED)"
   -> Renders itemized deduction breakdown, landlord signature line, and tenant signature line.
```

### Secondary Workflows
1. **Paid Payment Flow:** When status is recorded as `'Paid'`, no deposit deduction occurs. The receipt displays a green `"✓ PAID IN FULL"` stamp.
2. **Lease Expiry Enforcement:** If `rentalEndDate` is before `LocalDate.now()`, `isExpired()` evaluates to `true`. The tenant is moved to the "Inactive / Past Tenants" tab in `tenants/list.html`, and `PaymentService.savePayment()` throws an `IllegalArgumentException` blocking further payments.
3. **Duplicate Prevention:** If an owner tries to record a payment for the same tenant, month, and year twice, `PaymentRepository` detects the collision and aborts the save with an error message in `payments/form.html`.

---

## E. Core Technical Implementation

### Concept 1: Role-Based Access Control (RBAC) & Custom UserDetailsService
1. **What it is:** Restricting application URLs and operations based on user identity and privileges.
2. **Why needed:** Property owners must manage tenant lists and financial settings; tenants must only view their personal lease, deposit, and receipts without seeing other tenants' data.
3. **How it works internally:** Spring Security's `SecurityFilterChain` defines URL patterns. When a user submits credentials to `/login`, `CustomUserDetailsService.loadUserByUsername()` checks `OwnerAccountRepository`. If not found, it checks `TenantAccountRepository`. If found, it returns a Spring `UserDetails` object with either `ROLE_OWNER` or `ROLE_TENANT`. The `roleBasedSuccessHandler` redirects owners to `/tenants` and tenants to `/portal/dashboard`.
4. **Where implemented:** `src/main/java/com/rentreminder/app/config/SecurityConfig.java`, `CustomUserDetailsService.java`.
5. **Viva Question & Answer:**
   - *Question:* "How does your application prevent a tenant from manually typing `/tenants/new` in the browser bar?"
   - *Answer:* "In `SecurityConfig.java`, line 42 specifies `.requestMatchers(\"/tenants/**\", \"/payments/**\").hasRole(\"OWNER\")`. Spring Security's `AuthorizationFilter` intercepts the request before it reaches the controller. If an authenticated user with `ROLE_TENANT` attempts to access that route, an HTTP 403 Forbidden response is generated."

---

### Concept 2: Automated Invitation Auto-Linking (Event-Free Asynchronous Binding)
1. **What it is:** Decoupled association between two records created at different times by different actors.
2. **Why needed:** An owner adds a tenant before the tenant has registered an account. When the tenant signs up later, the system must link their new login credentials to their preexisting lease records without manual database updates.
3. **How it works internally:** When the owner saves a tenant, `TenantInvitationService.createInvitation()` saves a `TenantInvitation` storing the tenant's email. When the tenant registers via `TenantAccountService.register()`, the service hashes the password, persists the `TenantAccount`, and immediately executes `invitationRepository.findByTenantEmail(email)` to attach the foreign key `tenant_account_id` to all matching invitations.
4. **Where implemented:** `src/main/java/com/rentreminder/app/service/TenantAccountService.java:40-47`.
5. **Viva Question & Answer:**
   - *Question:* "What happens if a tenant registers before the landlord adds them?"
   - *Answer:* "The reverse check is implemented in `TenantInvitationService.createInvitation()` (lines 41-43). When the landlord enters the tenant's email, the service checks `tenantAccountRepository.findByEmail(email)`. If the account already exists, it links the `TenantAccount` to the `TenantInvitation` immediately at creation time."

---

### Concept 3: Financial Reconciliation & Deposit Deduction Engine
1. **What it is:** An automated business rule that deducts overdue rental amounts from the tenant's security deposit balance.
2. **Why needed:** In rental housing, when a tenant defaults on rent, the landlord claims arrears against the deposited collateral. Automating this eliminates bookkeeping errors.
3. **How it works internally:** In `PaymentService.savePayment()`, after `paymentRepository.save(payment)` executes, the code evaluates `if ("Overdue".equalsIgnoreCase(saved.getStatus()))`. If true, it calls `TenantInvitationService.deductFromDeposit()`. That method queries for the active `STATUS_APPROVED` invitation, retrieves the current `depositBalance`, calculates `Math.max(0.0, current - amount)`, and persists the updated balance.
4. **Where implemented:** `src/main/java/com/rentreminder/app/service/PaymentService.java:40-42` and `TenantInvitationService.java:127-135`.
5. **Viva Question & Answer:**
   - *Question:* "What protects against the deposit balance becoming a negative number if overdue rent exceeds the deposit?"
   - *Answer:* "In `TenantInvitationService.java` line 132, the deduction logic uses `Math.max(0.0, current - amount)`. This clamps the deposit balance at zero, preventing negative balances."

---

### Concept 4: Automated CI/CD Quality Gate (Fail-Fast Pipeline)
1. **What it is:** A continuous integration policy where automated tests act as an impassable barrier between source code and deployment.
2. **Why needed:** To guarantee that regressions or broken business logic can never be packaged into a container or pushed to a live server.
3. **How it works internally:** In `Jenkinsfile`, stage `'Test'` executes `bat 'mvn test'`. If any JUnit assertion fails, Maven exits with a non-zero exit code (e.g., code 1). Jenkins intercepts this exit code, flags the stage as `FAILURE`, and immediately terminates the pipeline. Subsequent stages (`Package`, `Docker Build`, `Docker Deploy`, `Tomcat Deploy`) are skipped.
4. **Where implemented:** `Jenkinsfile:44-50`, `docs/week10-continuous-testing.md`.
5. **Viva Question & Answer:**
   - *Question:* "How does Jenkins know that a Selenium test failed?"
   - *Answer:* "The `maven-surefire-plugin` runs the tests. If an unhandled assertion error occurs, Surefire logs the failure into an XML report and commands the Maven process to terminate with exit code 1. Jenkins pipeline step `bat 'mvn test'` receives this return code and halts execution."

---

### Concept 5: Page Object Model (POM) & Failure Screenshot Interception
1. **What it is:** A design pattern that abstracts web UI pages into reusable Java classes, coupled with test lifecycle listeners for visual debugging.
2. **Why needed:** Hardcoding locators directly inside test methods makes tests brittle when UI changes. Furthermore, headless browser tests run without visible screens, making screenshots necessary to diagnose failures.
3. **How it works internally:** Each UI page has a class (e.g., `LoginPage`, `OwnerTenantsPage`) encapsulating `By` locators and user actions. The test classes extend `BaseSeleniumTest` which is annotated with `@ExtendWith(ScreenshotWatcher.class)`. `ScreenshotWatcher` implements JUnit 5's `AfterTestExecutionCallback`. If `context.getExecutionException()` is present, it casts `WebDriver` to `TakesScreenshot`, saves a PNG to `target/screenshots/`, and feeds the path to `SeleniumReportGenerator.java`.
4. **Where implemented:** `src/test/java/.../selenium/base/ScreenshotWatcher.java` and `pages/*.java`.
5. **Viva Question & Answer:**
   - *Question:* "Why use `AfterTestExecutionCallback` instead of `@AfterEach` for capturing failure screenshots?"
   - *Answer:* "In JUnit 5, `@AfterEach` teardown methods run after test execution and might alter the browser state or quit the driver. `AfterTestExecutionCallback` executes immediately when a test throws an exception, capturing the exact DOM state at the instant of failure before teardown executes."

---

### Concept 6: Multi-Stage Containerization & State Isolation
1. **What it is:** Splitting Docker image creation into build and runtime environments while separating application binaries from persistent state.
2. **Why needed:** Build tools (Maven, compiler) inflate container image sizes and introduce security vulnerabilities. Furthermore, container filesystems are ephemeral; restarting a container would wipe SQLite database files without volume mounting.
3. **How it works internally:** `Dockerfile` Stage 1 (`maven:3.9.6-eclipse-temurin-17 AS builder`) compiles the WAR. Stage 2 (`eclipse-temurin:17-jre-jammy`) copies only `app.war`. A dedicated non-root user `appuser` (UID 1000) is created. The database path is set to `/app/data/rentdb.sqlite`. In `docker run` or Jenkinsfile, `-v rent_data:/app/data` mounts a Docker named volume to `/app/data`.
4. **Where implemented:** `Dockerfile:1-63`, `Jenkinsfile:151`.
5. **Viva Question & Answer:**
   - *Question:* "If your Docker container crashes or is upgraded, does the rent data get deleted?"
   - *Answer:* "No. SQLite writes to `/app/data/rentdb.sqlite`. In the deployment command, `-v rent_data:/app/data` mounts a named Docker volume managed outside the container's lifecycle. Container destruction does not destroy the volume."

---

### Concept 7: Atomic Symlink Release Engine & Automated Rollback
1. **What it is:** Zero-downtime server deployment mechanism that switches live application versions by pointing a symbolic link to immutable release directories.
2. **Why needed:** Replacing a running WAR file directly in-place causes classloader corruption, file locking errors on Windows/Linux, and downtime during file transfer.
3. **How it works internally:** Ansible provisions `/opt/rent-portal/releases/{{ version }}/app.war`. It updates symlink `/opt/rent-portal/previous` to point to the old release, and updates `/opt/rent-portal/current` to point to the new release, followed by restarting `rent-portal.service`. If the new release fails health checks, `rollback.yml` repoints `current` to `previous` and restarts the service in under 2 seconds.
4. **Where implemented:** `ansible/site.yml`, `ansible/roles/portal_release/tasks/main.yml`, `ansible/rollback.yml`.
5. **Viva Question & Answer:**
   - *Question:* "How does the symlink mechanism achieve instantaneous rollback?"
   - *Answer:* "Because the previous release's files are never overwritten—they remain intact in `/opt/rent-portal/releases/<old_version>/`. Rolling back requires only updating the Linux symlink `current` to point back to the previous release folder and issuing a `systemctl restart`, with no re-upload or re-compilation needed."

---

## F. Important Source Files

| # | File Path | Main Responsibility | Key Methods / Symbols | How it Connects to Others | Viva Key Detail to Remember |
| :- | :--- | :--- | :--- | :--- | :--- |
| **1** | `config/SecurityConfig.java` | Defines authentication rules, public routes, authorization filters, and role redirection. | `filterChain()`, `passwordEncoder()`, `roleBasedSuccessHandler()` | Configures Spring Security filter chain; delegates user lookup to `CustomUserDetailsService`. | Uses custom `AuthenticationSuccessHandler` to route owners to `/tenants` and tenants to `/portal/dashboard`. |
| **2** | `config/CustomUserDetailsService.java` | Bridges Spring Security with JPA database accounts. | `loadUserByUsername(String email)` | Queries `OwnerAccountRepository` first; if absent, queries `TenantAccountRepository`. | Email is treated as the username; assigns `ROLE_OWNER` or `ROLE_TENANT`. |
| **3** | `model/TenantInvitation.java` | Domain model representing the onboarding contract, lease duration, and deposit ledger. | `isExpired()`, `getDepositBalance()`, `STATUS_APPROVED` | Links `Tenant` entity with `TenantAccount` login; manipulated by `TenantInvitationService`. | Has `isExpired()` logic: evaluates `rentalEndDate.isBefore(LocalDate.now())`. |
| **4** | `service/TenantInvitationService.java` | Core business logic for invitation lifecycles, active lease filtering, and deposit balances. | `createInvitation()`, `approve()`, `deductFromDeposit()`, `isTenantLeaseExpired()` | Injected into `TenantController`, `TenantPortalController`, and `PaymentService`. | Contains `deductFromDeposit()` which executes `Math.max(0.0, current - amount)`. |
| **5** | `service/PaymentService.java` | Manages rent payment persistence, validation checks, and triggers arrears deductions. | `savePayment()`, `getAllPayments()`, `findById()` | Calls `TenantInvitationService` to check lease expiry and deduct overdue amounts; uses `PaymentRepository`. | Throws `IllegalArgumentException` if lease is expired or if payment for month/year already exists. |
| **6** | `controller/PaymentController.java` | MVC controller for owner payment recording, summary tables, and printable receipts. | `savePayment()`, `showSummary()`, `downloadReceipt()` | Interacts with `PaymentService` and `TenantService`; renders `payments/form`, `summary`, and `receipt`. | Form validation errors reject duplicates and redisplay filtered active tenants. |
| **7** | `controller/TenantPortalController.java` | Controller governing the self-service tenant portal. | `register()`, `dashboard()`, `approveInvite()`, `viewReceipt()` | Uses `TenantAccountService`, `TenantInvitationService`, and `PaymentRepository`. | Implements security check ensuring tenants can only view receipts belonging to their own apartment. |
| **8** | `selenium/base/BaseSeleniumTest.java` | Foundation class for automated UI regression tests. | `setUpBase()`, `tearDownBase()`, `waitForVisibility()`, `captureManualScreenshot()` | Starts Spring Boot on `RANDOM_PORT`, configures headless Chrome with Edge fallback. | Annotated with `@ExtendWith(ScreenshotWatcher.class)` and `@SpringBootTest(webEnvironment = RANDOM_PORT)`. |
| **9** | `selenium/base/ScreenshotWatcher.java` | Test lifecycle watcher capturing screenshots upon failure. | `afterTestExecution()`, `testFailed()`, `captureScreenshot()` | Intercepts exceptions via JUnit 5; calls Selenium `TakesScreenshot`; feeds `SeleniumReportGenerator`. | Saves PNGs to `target/screenshots/` with class name, method name, and timestamp. |
| **10** | `Jenkinsfile` | Declarative CI/CD pipeline definition as code. | `pipeline`, `parameters`, `stages ('Test', 'Package', 'Docker Deploy')` | Automates Git checkout, Maven compilation, Selenium regression testing, Docker builds, and deployment. | The `Test` stage is an impassable quality gate: test failure aborts packaging and deployment. |
| **11** | `Dockerfile` | Multi-stage container recipe for reproducible builds. | `FROM maven:... AS builder`, `FROM eclipse-temurin:...` | Builds WAR artifact in stage 1; runs WAR as non-root user `appuser` on port 8081 in stage 2. | Includes `HEALTHCHECK` command: `curl -f http://localhost:8081/login || exit 1`. |
| **12** | `ansible/roles/portal_release/tasks/main.yml` | Declarative task execution for zero-downtime symlink deployment. | `ansible.builtin.file (symlink)`, `ansible.builtin.systemd` | Deploys release WAR, manages `current` and `previous` symlinks, restarts systemd daemon. | Enables instantaneous recovery via `rollback.yml` by pointing `current` to `previous`. |

---

## G. Database and API Details

### Database Architecture: Why SQLite?
- **Choice & Justification:** SQLite (`org.xerial:sqlite-jdbc`) was selected for simplicity, portability, and zero-configuration serverless persistence (`rentdb.sqlite`). It requires no independent daemon process, fitting within course lab VM memory constraints.
- **Dialect Adaptation:** Spring Boot 3 / Hibernate 6 does not include SQLite dialects natively. The project incorporates `hibernate-community-dialects` (`org.hibernate.community.dialect.SQLiteDialect`) in `pom.xml`.
- **Production Portability:** `pom.xml` also includes `mysql-connector-j`. Staging/production deployments can switch to MySQL by overriding `spring.datasource.url` via environment variables without code modification.

### Entity Relationship Model (ERD)

```
  +-------------------------+             +--------------------------+
  |      owner_accounts     |             |      tenant_accounts     |
  +-------------------------+             +--------------------------+
  | id (PK, Long)           |             | id (PK, Long)            |
  | name (String)           |             | name (String)            |
  | email (String, Unique)  |             | email (String, Unique)   |
  | password_hash (String)  |             | phone (String)           |
  +-------------------------+             | password_hash (String)   |
                                          +-------------+------------+
                                                        | 1
                                                        |
                                                        | 0..*
                                          +-------------v------------+
  +-------------------------+             |    tenant_invitations    |
  |         tenants         |             +--------------------------+
  +-------------------------+             | id (PK, Long)            |
  | id (PK, Long)           | 1           | tenant_id (FK, Long)     |
  | name (String)           |<------------| tenant_account_id (FK)   |
  | email (String)          |      0..*   | tenant_email (String)    |
  | phone (String)          |             | status (PENDING/APPROVED)|
  | apartment_number (String|             | deposit_amount (Double)  |
  +------------+------------+             | deposit_balance (Double) |
               | 1                        | rental_start_date (Date) |
               |                          | rental_end_date (Date)   |
               | 0..*                     | created_at (Date)        |
  +------------v------------+             +--------------------------+
  |         payments        |
  +-------------------------+
  | id (PK, Long)           |
  | tenant_id (FK, Long)    |
  | amount (Double)         |
  | date (LocalDate)        |
  | month (Integer, 1-12)   |
  | year (Integer)          |
  | status (Paid / Overdue) |
  +-------------------------+
```

### Key API / HTTP Routes

| Verb | Path | Role Access | Controller Method | Responsibility |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/` or `/home` | Public | `HomeController.home` | Smart redirect: authenticated owners to `/tenants`, tenants to `/portal/dashboard`. |
| `GET` | `/login` | Public | `HomeController.loginPage` | Renders unified login screen (`login.html`). |
| `POST`| `/login` | Public | Spring Security Filter | Authenticates credentials; routes via `roleBasedSuccessHandler`. |
| `POST`| `/logout` | Authenticated | Spring Security Filter | Terminates session; redirects to `/login?logout`. |
| `GET` | `/tenants` | `ROLE_OWNER` | `TenantController.listTenants` | Renders Active and Expired tenant tabs with invitation statuses. |
| `GET` | `/tenants/new` | `ROLE_OWNER` | `TenantController.showAddForm` | Displays form to add new tenant. |
| `POST`| `/tenants` | `ROLE_OWNER` | `TenantController.saveTenant` | Validates & persists tenant; auto-dispatches portal invitation. |
| `GET` | `/payments/summary` | `ROLE_OWNER` | `PaymentController.showSummary`| Lists all recorded payments across all properties. |
| `GET` | `/payments/new` | `ROLE_OWNER` | `PaymentController.showPaymentForm`| Displays payment form populated only with active tenants. |
| `POST`| `/payments` | `ROLE_OWNER` | `PaymentController.savePayment` | Validates payment, prevents duplicates, triggers deposit deduction if overdue. |
| `GET` | `/payments/receipt/{id}` | `ROLE_OWNER` | `PaymentController.downloadReceipt`| Generates printable receipt for a specific payment. |
| `GET` | `/portal/register` | Public | `TenantPortalController.showRegisterForm`| Renders tenant account registration page. |
| `POST`| `/portal/register` | Public | `TenantPortalController.register` | Validates password matching, creates account, links pending invites. |
| `GET` | `/portal/dashboard`| `ROLE_TENANT`| `TenantPortalController.dashboard`| Renders tenant dashboard with invitations, active leases, payments. |
| `POST`| `/portal/invite/{id}/approve` | `ROLE_TENANT` | `TenantPortalController.approveInvite`| Activates lease, configures deposit and start/end dates. |
| `POST`| `/portal/invite/{id}/reject` | `ROLE_TENANT` | `TenantPortalController.rejectInvite` | Sets invitation status to `REJECTED`. |
| `GET` | `/portal/receipt/{id}` | `ROLE_TENANT` | `TenantPortalController.viewReceipt`| Renders printable receipt verified against the tenant's own lease. |

### Validation, Consistency & Error Handling
1. **Input Validation:** Enforced using Jakarta constraints (`@NotBlank`, `@Min(1)`, `@NotNull`). If binding fails, `BindingResult.hasErrors()` returns the user to the form with field-specific alert messages.
2. **Business Rule Consistency:**
   - Preventing payments for expired leases: Handled in `PaymentService.java` with custom exception message.
   - Preventing duplicate payments: `PaymentRepository.findByTenantAndMonthAndYear` blocks re-recording rent for the same period.
3. **Receipt Security Guard:** In `TenantPortalController.viewReceipt()`, the application checks whether the requested payment's tenant ID matches one of the authenticated tenant's approved leases. If not, it redirects back to the dashboard, preventing unauthorized data access.

---

## H. Algorithms and Important Design Decisions

### 1. Account-to-Invitation Auto-Discovery Algorithm
- **Problem:** Owners create tenant records before tenants register. How do we link them without complex lookup tokens or email verification servers?
- **Mechanism:**
  - When owner creates tenant: `TenantInvitation` is persisted with `tenant_email = toLowerCase(trim(email))`.
  - When tenant registers: System queries `invitationRepository.findByTenantEmail(email)`. For each matched invitation where `tenantAccount == null`, it binds the foreign key to the new `TenantAccount`.
- **Complexity:** $O(N)$ where $N$ is the number of invitations matching the email (typically 1 or 2). Database lookup is $O(1)$ on indexed columns.
- **Trade-off:** Avoids needing an external SMTP mailing server in lab environments while ensuring seamless account linking upon registration.

### 2. Leased Period Expiration & Deposit Clamping Algorithm
- **Mechanism:**
  ```java
  public boolean isExpired() {
      return STATUS_APPROVED.equalsIgnoreCase(this.status)
              && this.rentalEndDate != null
              && this.rentalEndDate.isBefore(LocalDate.now());
  }
  ```
  When rent is marked Overdue:
  ```java
  double current = invite.getDepositBalance() != null ? invite.getDepositBalance() : 0.0;
  invite.setDepositBalance(Math.max(0.0, current - amount));
  ```
- **Complexity:** $O(1)$ time and $O(1)$ space.
- **Trade-off:** Calculating lease expiration dynamically on request avoids the need for a background cron job thread, keeping memory consumption low.

### 3. Page Object Model (POM) in Automated Testing
- **Design Pattern:** Structural separation between test logic (`journeys/*.java`) and web page DOM interactions (`pages/*.java`).
- **Why chosen:** If a form element ID changes in Thymeleaf templates (e.g., `#pay-amount`), the locator is updated in a single place (`PaymentFormPage.java`) rather than across multiple test files.

### 4. Headless Browser Execution with Cross-Browser Fallback
- **Mechanism:** `BaseSeleniumTest.java` configures `ChromeOptions` with `--headless=new`, `--no-sandbox`, `--disable-dev-shm-usage`, and `--disable-gpu`. If Chrome initialization fails (e.g., missing binary), the `catch` block falls back to `EdgeDriver` with equivalent flags.
- **Trade-off:** Headless mode runs in CI environments without a GUI display server (X11/Wayland), using 60% less memory than full browser rendering.

---

## I. Challenges, Limitations, and Future Scope

### Confirmed Technical Challenges Overcome During Development
1. **Hibernate 6 SQLite Compatibility:** Spring Boot 3.2+ uses Hibernate 6, which removed built-in SQLite support.
   - *Resolution:* Added `org.hibernate.orm:hibernate-community-dialects` and configured `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect`.
2. **Selenium DevTools Protocol (CDP) Mismatch in CI:** Chrome browser updates frequently trigger verbose CDP version warning logs that can pollute test outputs.
   - *Resolution:* Added specific logging suppressions in `application.properties`:
     `logging.level.org.openqa.selenium.devtools=OFF`
     `logging.level.org.openqa.selenium.chromium.ChromiumDriver=ERROR`.
3. **Windows/Linux File Locking in CI/CD:** Overwriting a running WAR file on Windows locks the file and causes deployment failures.
   - *Resolution:* Replaced in-place file overwrites with Docker containerization (`docker rm -f` followed by fresh run) and Ansible atomic symlink switches (`/opt/rent-portal/current`).

### Current System Limitations
1. **Concurrency Bottlenecks with SQLite:** SQLite locks the entire database file during write transactions. While sufficient for single-landlord use, high concurrent write operations will cause `SQLITE_BUSY` lock errors.
2. **Missing Real SMTP Gateway:** Outbound notification emails are simulated through in-app invitation linking rather than sending actual emails via JavaMailSender.
3. **No Digital Payment Gateway:** Rent payments are recorded as bookkeeping entries by the owner; real money transfers (UPI/Stripe/Razorpay) are not integrated.

### Future Scope & Practical Enhancements
1. **Database Migration to Managed Postgres/MySQL:** Switch datasource configurations to PostgreSQL with connection pooling (HikariCP) for production concurrency.
2. **Payment Gateway Integration:** Incorporate Razorpay or Stripe webhooks to allow tenants to pay rent directly from their portal dashboard.
3. **Automated WhatsApp / SMS Reminders:** Integrate Twilio or WhatsApp Business API to dispatch automated rent reminders 3 days before the due date.
4. **Kubernetes Orchestration:** Migrate Docker deployment to Kubernetes (K8s) manifests with Helm charts, Horizontal Pod Autoscaling (HPA), and persistent volume claims (PVC).

---

## J. Viva Question Bank (20 High-Probability Questions & Answers)

### Category 1: Problem Statement, Objectives & Features
1. **Q: What problem does this portal solve compared to generic property management software?**  
   *A:* It provides a streamlined solution for small-scale landlords and PG owners, eliminating manual notebooks. It features an automated onboarding workflow, self-service tenant lease approval, and automatic deposit deductions when rent is overdue, backed by a production-grade CI/CD pipeline.
2. **Q: Who are the primary actors in the system and how are they separated?**  
   *A:* Property Owners (`ROLE_OWNER`) and Tenants (`ROLE_TENANT`). Owners have administrative control over tenant directories and payment logging. Tenants have a self-service portal to approve leases, review deposit balances, and download receipts.
3. **Q: What happens if an owner marks a payment as "Overdue"?**  
   *A:* In `PaymentService.java`, the system records the payment and calls `TenantInvitationService.deductFromDeposit()`, which deducts that rent amount from the tenant's security deposit balance down to a minimum of zero.
4. **Q: How does the application prevent duplicate rent entries for the same month?**  
   *A:* `PaymentRepository.findByTenantAndMonthAndYear()` checks for an existing record matching the tenant, month, and year. If one exists, `PaymentService` throws an `IllegalArgumentException` and rejects the form submission.
5. **Q: How are expired leases handled in the owner's dashboard?**  
   *A:* In `tenants/list.html`, tenants are partitioned into two tabs: "Active Tenants" and "Inactive / Past Tenants". The inactive tab shows tenants whose `rentalEndDate` has passed. The payment form also filters out expired tenants to prevent recording rent for ended leases.

### Category 2: Architecture & Technology Stack
6. **Q: Why was Spring Boot 3.2 selected instead of earlier Spring versions?**  
   *A:* Spring Boot 3 provides baseline compatibility with Java 17, Spring Framework 6, Jakarta EE 10 APIs, enhanced security defaults, and improved native image and container performance.
7. **Q: Why use SQLite instead of a standalone database server like MySQL?**  
   *A:* SQLite is a serverless, zero-configuration embedded database stored as a single file (`rentdb.sqlite`). This makes it lightweight and portable for lab environments and Docker containers. The application also includes `mysql-connector-j` so production environments can switch to MySQL via environment variables.
8. **Q: Explain the role of Thymeleaf in this architecture. Why not React or Angular?**  
   *A:* Thymeleaf is a server-side template engine that processes natural HTML templates directly within the Spring Boot JVM. It eliminates the need for Node.js build pipelines and CORS configurations while integrating smoothly with Spring Security tag libraries.
9. **Q: What is the purpose of the multi-stage Dockerfile?**  
   *A:* Stage 1 uses a full Maven image to compile the application from source. Stage 2 copies only the compiled WAR into a minimal JRE runtime (`eclipse-temurin:17-jre-jammy`). This reduces the final image size and eliminates build tools from the production container to minimize security vulnerabilities.
10. **Q: How does the Ansible deployment achieve zero-downtime and instant rollback?**  
    *A:* By using an atomic symlink architecture. Each release is placed in `/opt/rent-portal/releases/<version>/`. A symlink (`current`) points to the active version. Rolling back simply requires pointing `current` back to `previous` and restarting the systemd service.

### Category 3: Implementation Details & Algorithms
11. **Q: How does `CustomUserDetailsService` authenticate both owners and tenants with one login form?**  
    *A:* It queries `OwnerAccountRepository.findByEmail()`. If found, it returns a user with `ROLE_OWNER`. If not, it queries `TenantAccountRepository.findByEmail()` and returns `ROLE_TENANT`. If neither exists, it throws `UsernameNotFoundException`.
12. **Q: How does BCrypt password hashing work in your system?**  
    *A:* In `SecurityConfig.java`, we define a `BCryptPasswordEncoder` bean. BCrypt uses a salted, adaptive key derivation function with an internal work factor (default 10). When a tenant registers or default owner data seeds, the plaintext password is encrypted before saving to the database.
13. **Q: How does the auto-linking mechanism work when a tenant registers after being invited?**  
    *A:* When `TenantAccountService.register()` persists a new tenant, it queries `invitationRepository.findByTenantEmail()`. For any matching pending invitations, it attaches the new `TenantAccount` reference to the `TenantInvitation` record.
14. **Q: What design pattern did you use for automated testing and why?**  
    *A:* The Page Object Model (POM). Page representations (`LoginPage`, `TenantDashboardPage`) encapsulate DOM locators and browser interactions. Test classes (`Journey1`–`Journey5`) contain only test logic and assertions, improving code maintainability.
15. **Q: Explain how `ScreenshotWatcher` captures screenshots when a test fails.**  
    *A:* It implements JUnit 5's `AfterTestExecutionCallback`. When an exception is detected via `context.getExecutionException()`, it casts the WebDriver instance to `TakesScreenshot`, saves a PNG to `target/screenshots/`, and records the relative path in `SeleniumReportGenerator`.

### Category 4: Challenging Follow-Up Questions (Viva Examiner Mode)
16. **Q: What happens if two owners attempt to record rent for the same tenant at the exact same millisecond?**  
    *A:* SQLite handles writes by obtaining an exclusive file lock (`EXCLUSIVE lock`). The second transaction will wait until the busy timeout expires. If both pass the duplicate check in memory simultaneously, database-level uniqueness constraints or transactional rollback in Spring Data JPA will prevent duplicate records.
17. **Q: In your Dockerfile, why do you create `appuser` and switch to it using `USER appuser`?**  
    *A:* Running containerized applications as `root` violates the Principle of Least Privilege. If an attacker breaches the application via a remote code vulnerability, root privileges inside the container could allow container breakout attacks on the host kernel.
18. **Q: If a Jenkins build fails during the `mvn test` stage, what happens to the running production container?**  
    *A:* Nothing. The pipeline enforces a fail-fast quality gate. Because `mvn test` exits with code 1, the pipeline halts immediately. The `Docker Build` and `Docker Deploy` stages never execute, ensuring the live container continues running the last known good build.
19. **Q: How do you prevent CSRF (Cross-Site Request Forgery) attacks on state-changing POST requests?**  
    *A:* Spring Security enables CSRF tokens by default. In our Thymeleaf forms, `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>` embeds a unique session token that Spring Security validates on every POST request.
20. **Q: If you replace SQLite with MySQL in production, what exact files must be updated?**  
    *A:* Only configuration files, with zero Java code changes:
    1. In `application.properties`, uncomment the MySQL datasource URL, username, and password properties.
    2. In `pom.xml`, `mysql-connector-j` is already included.
    3. In Docker/Ansible, pass `SPRING_DATASOURCE_URL`, `DB_USER`, and `DB_PASS` as environment variables.

---

## K. Last-Minute Viva Revision Sheet

### 1. 60-Second Opening Pitch
> *"Our project is the Rent Payment Reminder Portal, a full-stack Java 17 and Spring Boot 3 web application integrated with an automated DevOps CI/CD pipeline. It solves tracking and accountability issues in residential rentals by providing role-based portals for landlords and tenants. Core features include tenant onboarding, lease agreement approval, rent tracking with duplicate prevention, and automated security deposit deductions when rent is marked overdue. The project features an automated quality gate using a 10-scenario Selenium UI regression suite in Jenkins, unprivileged multi-stage Docker containerization with persistent volumes, and Ansible playbooks for zero-downtime symlink deployment and automated rollback."*

### 2. 2-Minute Technical Deep Dive (Architecture & Flow)
> *"From an architectural standpoint, the application follows an MVC architecture using Spring Boot 3.2.5 and Thymeleaf. Authentication is governed by Spring Security 6, where `CustomUserDetailsService` unifies `OwnerAccount` and `TenantAccount` entities, using BCrypt password hashing and custom success handlers for role-based routing.
>
> When an owner onboards a tenant, an invitation record is automatically created in a PENDING state. When the tenant signs up, the system links the invitation by email address. Once the tenant approves the lease on their dashboard and configures their deposit, the lease becomes ACTIVE. When an owner records a payment, `PaymentService` enforces business rules: verifying the lease is not expired, checking for duplicates in that month/year, and deducting overdue rent from the tenant's security deposit balance via `TenantInvitationService`.
>
> On the DevOps side, our `Jenkinsfile` runs a declarative pipeline. The `Test` stage executes headless Chrome Selenium tests via Maven. If any assertion fails, the build halts immediately and captures failure screenshots via a JUnit 5 `AfterTestExecutionCallback`. Upon passing, the application is packaged into a WAR, built into an unprivileged Docker image using a multi-stage `Dockerfile`, and deployed with persistent SQLite named volumes, or deployed via Ansible using an atomic symlink release architecture."*

### 3. Compact Technology-to-Purpose Reference
- **Java 17 / Spring Boot 3.2.5:** Core backend runtime and enterprise application framework.
- **Spring Security 6:** BCrypt password encryption, session management, and role-based access control.
- **Spring Data JPA & Hibernate 6:** Object-relational mapping, custom repository queries, and schema generation.
- **SQLite & Community Dialect:** Lightweight serverless persistence using `rentdb.sqlite`.
- **Thymeleaf 3:** Natural server-side HTML rendering with CSRF token injection.
- **Selenium WebDriver 4.20.0:** Headless browser regression testing following Page Object Model design.
- **JUnit 5 Jupiter:** Test execution, parameterization, and screenshot callback listeners.
- **Jenkins Declarative Pipeline:** CI/CD pipeline-as-code enforcing automated testing quality gates.
- **Docker:** Multi-stage image packaging, non-root user execution, and named volume persistence.
- **Ansible:** Automated idempotent provisioning and atomic symlink zero-downtime deployments.

### 4. Ten Key Implementation Facts to Remember
1. **Default Owner Credentials:** `owner@rentportal.com` / `owner123` (seeded automatically by `DataSeeder.java`).
2. **Default Application Port:** Port `8081` (`server.port=8081` in `application.properties`).
3. **Database Filename:** `rentdb.sqlite` in the working root or `/app/data/` inside containers.
4. **Roles in System:** `ROLE_OWNER` and `ROLE_TENANT`.
5. **Overdue Deduction Math:** `invite.setDepositBalance(Math.max(0.0, current - amount))` in `TenantInvitationService.java:132`.
6. **Lease Expiry Method:** `TenantInvitation.isExpired()` checks `rentalEndDate.isBefore(LocalDate.now())`.
7. **Selenium Suite Size:** 10 scenarios covering 5 critical user journeys with 100% pass rate.
8. **Failure Screenshot Folder:** `target/screenshots/*.png`.
9. **Interactive Test Report:** Generated by `SeleniumReportGenerator.java` at `target/selenium-reports/index.html`.
10. **Docker Non-Root User:** `appuser:appgroup` running on minimal `eclipse-temurin:17-jre-jammy`.

### 5. Five Likely Follow-Up Questions from the Examiner
1. *"Why did you use SQLite instead of MySQL in a Spring Boot application?"*  
   $\rightarrow$ Emphasize zero-configuration portability for academic and lab containerization, noting that `mysql-connector-j` is included for production environments.
2. *"How does the system ensure a tenant cannot see another tenant's payment history?"*  
   $\rightarrow$ Point to `TenantPortalController.viewReceipt()` where payment records are verified against the authenticated tenant's approved lease before displaying.
3. *"What happens if a Selenium test fails in Jenkins?"*  
   $\rightarrow$ The pipeline halts immediately at the `Test` stage, skipping package and deployment to protect production.
4. *"Where does the data persist when the Docker container is restarted?"*  
   $\rightarrow$ Inside the Docker named volume `rent_data` mounted to `/app/data/`.
5. *"Why is the Docker image running as `appuser` instead of `root`?"*  
   $\rightarrow$ Adheres to the Principle of Least Privilege to prevent potential container breakout attacks.

### 6. Pre-Viva Checklist
- [ ] Review `SecurityConfig.java`, `PaymentService.java`, and `TenantInvitationService.java`.
- [ ] Be ready to draw the architecture diagram (Boxes: Browser, Spring Security, Controllers, Services, Repositories, Database, Docker/Jenkins).
- [ ] Practice the 60-second opening pitch aloud.
- [ ] Review how `Math.max(0.0, current - amount)` protects deposit balances.
- [ ] Review the Jenkins pipeline flow: Checkout $\rightarrow$ Compile $\rightarrow$ Test (Quality Gate) $\rightarrow$ Package $\rightarrow$ Docker Build $\rightarrow$ Deploy.
