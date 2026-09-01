# Rent Payment Reminder Portal

## 1. Problem Statement & Scope (Week 1)

Many tenants and small landlords track rent manually (WhatsApp, notebooks,
spreadsheets), which leads to missed due dates, disputed payment history,
and no single source of truth for who has paid. The **Rent Payment
Reminder Portal** is a lightweight web app that lets a landlord/PG owner
record tenants, log rent transactions, validate entries, view a payment
summary, and download a status/receipt — with automated reminders as a
stretch goal.

**Target users:** individual landlords / PG or flat-share managers with a
small number of tenants (5–30).

**Pain points addressed:**
- No centralized, tamper-evident record of who paid what and when
- No quick way to see who is overdue
- No downloadable proof of payment for tenants

**Stakeholders:** landlord/admin (primary user), tenants (indirect
beneficiaries), instructor/evaluator (project stakeholder).

**Constraints:** 15-week academic timeline, single-developer team, must
demonstrate the full DevOps toolchain (Git/GitHub → Jenkins → Selenium →
Docker → Ansible/Puppet), so the application scope is intentionally kept
small.

**MVP scope (frozen):**
1. User/account setup (landlord login, add tenant)
2. Transaction entry (record a rent payment)
3. Validation (amount, date, duplicate-payment checks)
4. Summary view (per-tenant and overall payment status)
5. Downloadable status/receipt (PDF or printable view)

**Out of scope for MVP:** SMS/email reminders, online payment gateway,
multi-property support — noted as future enhancements.

**Success criteria:** a landlord can add a tenant, log a payment, see it
reflected in the summary, and download a receipt — end to end, deployed
via the CI/CD pipeline built in this project.

## 2. Tech Stack (Week 3)

| Layer | Choice |
|---|---|
| Language / Framework | Java 17, Spring Boot 3 |
| Build tool | Maven |
| View layer | Thymeleaf + plain CSS/JS |
| Database | H2 (local/dev), MySQL (staging/prod) |
| Deployment target | Apache Tomcat (WAR), containerized later with Docker |
| Testing | JUnit, Selenium WebDriver |
| CI/CD | Jenkins (Jenkinsfile, pipeline-as-code) |
| Config management | Ansible / Puppet (Week 17-18) |

### Local setup
```bash
mvn spring-boot:run
# App runs at http://localhost:8080
```

### Build a deployable WAR
```bash
mvn clean package
# Output: target/rent-reminder-portal.war
```

## 3. Architecture (Week 3)

```
Browser
   │
   ▼
Spring MVC Controller  ──►  Service Layer  ──►  Repository (Spring Data JPA)
   │                                                     │
   ▼                                                     ▼
Thymeleaf Views                                    H2 / MySQL Database
```

**Use-case summary:** Landlord logs in → adds/edits tenant → records rent
payment → system validates entry → summary dashboard updates → landlord
downloads receipt.

**Planned data model (MVP):**
- `Tenant` (id, name, contact, room/unit, monthlyRent)
- `Payment` (id, tenantId, amountPaid, paymentDate, month, status)

**Planned API/routes (MVP):**
- `GET /` — landing/dashboard
- `GET /tenants`, `POST /tenants` — list/add tenants
- `GET /payments`, `POST /payments` — list/record payments
- `GET /payments/{id}/receipt` — download receipt

## 4. Repository & Branching Policy (Week 4)

**Folder structure:**
```
src/main/java/com/rentreminder/app/
  ├── controller/   Spring MVC controllers
  ├── service/      Business logic
  ├── repository/   Spring Data JPA repositories
  ├── model/        JPA entities
  └── config/       App configuration
src/main/resources/
  ├── templates/    Thymeleaf views
  └── static/       CSS / JS
src/test/java/       Unit + Selenium tests
docs/                Diagrams, SRS notes, reports
.github/ISSUE_TEMPLATE/
```

**Branch naming rules:**
- `main` — always deployable
- `develop` — integration branch
- `feature/<short-description>` — e.g. `feature/tenant-onboarding`
- `bugfix/<short-description>`
- `release/<version>`

**Workflow:** feature branches → PR into `develop` → review → merge →
periodic release branch merged into `main` and tagged.

## Weekly Progress Log

| Week | Task | Status |
|---|---|---|
| 1 | Problem definition & scope | ✅ Done |
| 2 | Agile planning & DevOps workflow | ✅ Done |
| 3 | Architecture & tech setup | ✅ Done |
| 4 | Git/GitHub repo initialization | ✅ Done |
| 5 | Feature branch development | ⏳ Upcoming |
