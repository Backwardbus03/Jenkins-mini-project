# Week 2 — Agile Planning & DevOps Workflow

## Product Backlog (User Stories)

| ID | User Story | Priority |
|---|---|---|
| US-1 | As a landlord, I can create an account/login so my data is private | High |
| US-2 | As a landlord, I can add a tenant with room and monthly rent | High (Done) |
| US-3 | As a landlord, I can record a rent payment for a tenant | High (Done) |
| US-4 | As the system, I validate payment entries (no negative/duplicate amounts) | High (Done) |
| US-5 | As a landlord, I can view a summary of who has paid / is overdue | High (Done) |
| US-6 | As a landlord, I can download a receipt/status for a payment | Medium (Done) |
| US-7 | As a landlord, I can edit or remove a tenant record | Medium |
| US-8 | As a landlord, I get a reminder list of upcoming due dates | Low (stretch) |

**Acceptance criteria example (US-3):**
- Given a valid tenant, when I submit amount + date, then a payment record is created and appears in the summary.
- Given an empty or negative amount, when I submit, then the form shows a validation error and no record is created.

## Definition of Done
A task is "Done" when:
1. Code is committed to a feature branch with a clear message
2. Feature builds successfully (`mvn clean package`)
3. Manually verified against the acceptance criteria
4. Merged into `develop` via reviewed pull request
5. No known critical defects open against it

## 15-Week Kanban / Sprint Plan

| Week | Focus |
|---|---|
| 1 | Problem definition & scope |
| 2 | Agile planning & DevOps workflow |
| 3 | Architecture & tech setup |
| 4 | GitHub repo initialization |
| 5 | Feature branch: core workflow #1 |
| 6 | MVP completion, merge conflict demo, tagging |
| 7 | Jenkins install + CI job |
| 8 | Jenkinsfile pipeline + Tomcat deploy |
| 9 | Selenium test design + local run |
| 10 | Continuous testing in Jenkins |
| 11 | Dockerfile + container lifecycle |
| 12 | Jenkins → Docker CD pipeline |
| 13 | Ansible/Puppet configuration script |
| 14 | Automated provisioning + rollback test |
| 15 | Final end-to-end release, docs, viva |

Board columns: **Backlog → In Progress → In Review → Done**

## DevOps Lifecycle (Dev → Ops)

```
 Plan → Code → Build → Test → Release → Deploy → Operate → Monitor
  │       │       │      │        │         │         │        │
 Backlog  Git   Maven  Selenium  Jenkins  Tomcat/    Container  Health
 (Agile) commit  build   tests   pipeline  Docker     running   checks,
                                            deploy               logs
                                                ▲___________________│
                                                 feedback loop into Plan
```
