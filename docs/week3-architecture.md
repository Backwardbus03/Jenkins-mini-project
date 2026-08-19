# Week 3 — Requirements, Architecture & Technology Setup

## SRS Summary

**Purpose:** Provide a landlord/PG owner a simple way to track tenants
and rent payments, replacing manual notebooks/WhatsApp tracking.

**Functional requirements:**
- FR1: System shall allow landlord login/account setup
- FR2: System shall allow adding, editing, listing tenants
- FR3: System shall allow recording a rent payment against a tenant
- FR4: System shall validate payment entries (amount > 0, valid date, no duplicate entry for same tenant+month)
- FR5: System shall show a summary view (paid / overdue per tenant)
- FR6: System shall generate a downloadable receipt/status for a payment

**Non-functional requirements:**
- NFR1: Should run on a single small VM/container (course lab constraint)
- NFR2: Should be deployable via Jenkins pipeline to Tomcat and later Docker
- NFR3: Basic input validation to prevent bad data entry

## Use-Case Diagram (textual)

```
                +------------------------------+
                |   Rent Payment Reminder App   |
                |                                |
 (Landlord) ----|--> Login / Account Setup       |
     |          |--> Add / Edit Tenant           |
     |          |--> Record Payment              |
     |          |--> View Summary                |
     |          |--> Download Receipt            |
     |          +------------------------------+
     |
     v
 (Tenant - indirect beneficiary, receives receipt info from landlord)
```

## Data Model

**Tenant**
| Field | Type |
|---|---|
| id | Long (PK) |
| name | String |
| contact | String |
| roomOrUnit | String |
| monthlyRent | BigDecimal |

**Payment**
| Field | Type |
|---|---|
| id | Long (PK) |
| tenantId | Long (FK → Tenant) |
| amountPaid | BigDecimal |
| paymentDate | LocalDate |
| forMonth | String (e.g. "2026-09") |
| status | Enum: PAID, PARTIAL, OVERDUE |

## API / Route List (MVP)

| Method | Route | Purpose |
|---|---|---|
| GET | `/` | Dashboard / landing |
| GET | `/tenants` | List tenants |
| POST | `/tenants` | Add tenant |
| PUT | `/tenants/{id}` | Edit tenant |
| GET | `/payments` | List payments |
| POST | `/payments` | Record payment |
| GET | `/payments/{id}/receipt` | Download receipt |
| GET | `/summary` | Paid/overdue summary |

## Local Development Setup
1. Install JDK 17 and Maven
2. Clone the repo, run `mvn spring-boot:run`
3. App available at `http://localhost:8080` using in-memory H2 DB (no external DB needed for local dev)
