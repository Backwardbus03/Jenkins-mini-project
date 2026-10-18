# Configuration Management, Automated Provisioning & Reliability Validation

**Project:** Rent Payment Reminder Portal  
**Scope:** Automated Configuration Management, Multi-Tier Reliability, Zero-Downtime Deployment, and Rollback/Recovery Validation using Ansible.

---

## 1. Executive Summary & Architecture

This document establishes the enterprise configuration specification and automated provisioning pipeline for the **Rent Payment Reminder Portal**. Using Ansible declarative playbooks and structured roles, the target node is automatically transformed from a minimal clean Linux node into a hardened, production-ready application server.

```
+---------------------------------------------------------------------------------------------------+
|                                 Ansible Orchestration Architecture                                |
|                                                                                                   |
|  +--------------------+                                         +------------------------------+  |
|  | Ansible Controller |   SSH (Key Auth + Sudo)                 | Clean Target Node (Ubuntu)   |  |
|  |                    | --------------------------------------> |                              |  |
|  | - site.yml         |                                         | 1. OS Packages (JDK 17, etc) |  |
|  | - rollback.yml     |                                         | 2. User & Group (rentportal) |  |
|  | - healthcheck.yml  |                                         | 3. Filesystem & Hardening    |  |
|  | - roles/           |                                         | 4. systemd Unit & Env file   |  |
|  +--------------------+                                         | 5. Symlink Release Engine    |  |
|                                                                 +------------------------------+  |
+---------------------------------------------------------------------------------------------------+
```

### Release Architecture: Atomic Symlink Switch & State Isolation
To guarantee high reliability, zero-downtime rollouts, and instant rollbacks, application artifacts are isolated from runtime data:

```
/opt/rent-portal/
├── current  ----------------------------> releases/1.0.0/ (or 1.0.1)
├── previous ----------------------------> releases/1.0.0/ (last stable target)
└── releases/
    ├── 1.0.0/
    │   └── app.war
    └── 1.0.1/
        └── app.war

/var/lib/rent-portal/data/                <-- Persistent State (survives rollbacks/upgrades)
└── rentdb.sqlite

/var/log/rent-portal/                     <-- Isolated logging + logrotate
└── app.log

/etc/rent-portal/                         <-- Environment config
└── rent-portal.env
```

---

## 2. Server Prerequisites & Configuration Specification

### 2.1 Operating System & Base Stack
| Item | Specification | Rationale |
| :--- | :--- | :--- |
| **Operating System** | Ubuntu 22.04 LTS (Jammy Jellyfish) / Debian 12 | Enterprise standard long-term support release |
| **Kernel / Init** | Linux with `systemd` as PID 1 | Modern service supervision, cgroup isolation, process restart policies |
| **Architecture** | `x86_64` (AMD64) | Standard server hardware architecture |

### 2.2 Package Requirements
| Package Name | Minimum Version | Category | Purpose |
| :--- | :--- | :--- | :--- |
| `openjdk-17-jre-headless` | 17.0.x | Runtime | Spring Boot 3.2.5 requires Java 17+ bytecode compatibility |
| `curl` | 7.81.0+ | Utilities | Automated HTTP health check probe and status telemetry |
| `sqlite3` | 3.37.0+ | Database Tools | Database diagnostics and command-line verification |
| `ca-certificates` | Latest | Security | TLS trust store for outbound notifications and updates |
| `logrotate` | 3.19.0+ | Maintenance | Automated rotation and compression of application logs |
| `ufw` | 0.36+ | Network Security | Uncomplicated Firewall for perimeter port filtering |

### 2.3 User & Group Specifications (Principle of Least Privilege)
| Identity | Type | Value | Shell / Home | Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **User** | System Service Account | `rentportal` | `/usr/sbin/nologin` / `/opt/rent-portal` | Prevents interactive shell login; isolated execution context |
| **Group** | System Group | `rentportal` | N/A | Group-level file access restrictions |

### 2.4 Filesystem Structure & Ownership Specifications
| Directory / File Path | Owner:Group | Permissions | Purpose |
| :--- | :--- | :--- | :--- |
| `/opt/rent-portal` | `root:rentportal` | `0755` | Base deployment root |
| `/opt/rent-portal/releases/` | `root:rentportal` | `0755` | Versioned immutable WAR release packages |
| `/opt/rent-portal/current` | `root:rentportal` | Symlink (`lrwxrwxrwx`) | Pointer to active live release directory |
| `/opt/rent-portal/previous` | `root:rentportal` | Symlink (`lrwxrwxrwx`) | Pointer to previous verified stable release |
| `/var/lib/rent-portal/data` | `rentportal:rentportal` | `0750` | Persistent SQLite database (`rentdb.sqlite`) |
| `/var/log/rent-portal` | `rentportal:rentportal` | `0750` | Application log output directory (`app.log`) |
| `/etc/rent-portal` | `root:rentportal` | `0750` | Configuration storage directory |
| `/etc/rent-portal/rent-portal.env` | `root:rentportal` | `0640` | Process environment variables file |
| `/etc/systemd/system/rent-portal.service` | `root:root` | `0644` | systemd daemon service definition |
| `/etc/logrotate.d/rent-portal` | `root:root` | `0644` | Automated log rotation rule |

### 2.5 Ports & Network Specifications
| Port Number | Protocol | Direction | Source / Destination | Service | Security Context |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **8081** | TCP | Inbound | Public / Reverse Proxy | Spring Boot Web Application | Portal web interface, REST APIs |
| **22** | TCP | Inbound | Control Node / Admin Bastion | OpenSSH Daemon | Configuration management via Ansible |

### 2.6 Services & Daemons
| Service Name | Init Manager | Startup State | Restart Policy | Supervised Path |
| :--- | :--- | :--- | :--- | :--- |
| `rent-portal.service` | systemd | Enabled (boot) | `Restart=on-failure`, `RestartSec=5` | `/usr/bin/java -jar /opt/rent-portal/current/app.war` |
| `ssh.service` | systemd | Enabled (boot) | Native systemd | `/usr/sbin/sshd` |
| `logrotate` | systemd / cron | Enabled (daily/weekly) | Native timer | `/usr/sbin/logrotate` |

### 2.7 Security & Hardening Directives in `rent-portal.service`
The systemd service unit implements Linux namespace and capability restrictions:
- `NoNewPrivileges=true`: Disables SUID execution within the child processes.
- `PrivateTmp=true`: Allocates an isolated `/tmp` namespace.
- `ProtectSystem=full`: Mounts `/usr`, `/boot`, and `/etc` as read-only for the service.
- `ProtectHome=true`: Completely hides `/home`, `/root`, and `/run/user`.
- `ReadWritePaths=/var/lib/rent-portal/data /var/log/rent-portal`: Strictly limits write operations only to data and log folders.
- `-Dorg.sqlite.tmpdir=/var/lib/rent-portal/data`: Guarantees SQLite JNI native libraries unpack and execute in authorized read-write storage even when `/tmp` is mounted with `noexec`.

---

## 3. Configuration Management Playbook Implementation

The configuration management code is located in the [`ansible/`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible) directory:

```
ansible/
├── ansible.cfg                          # Global Ansible settings, pipelining, sudo escalation
├── site.yml                             # Master provisioning and deployment playbook
├── rollback.yml                         # Atomic rollback and recovery playbook
├── healthcheck.yml                      # Standalone multi-vector reliability validation playbook
├── inventory/
│   └── hosts.yml                        # Target node inventory definition
├── group_vars/
│   └── rent_portal.yml                  # Variables: paths, ports, health settings, packages
├── roles/
│   ├── prerequisites/                   # Role 1: Base system setup
│   │   ├── tasks/main.yml               # Package installation, user creation, directories
│   │   ├── handlers/main.yml            # systemd daemon reload & service restarts
│   │   └── templates/
│   │       ├── rent-portal.env.j2       # Environment file template
│   │       ├── rent-portal.service.j2   # Hardened systemd unit template
│   │       └── rent-portal.logrotate.j2 # Logrotate policy template
│   └── portal_release/                  # Role 2: Immutable release & health check engine
│       ├── tasks/
│       │   ├── main.yml                 # Versioned directory creation, copy, symlink, rollback
│       │   └── healthcheck.yml          # Port wait and HTTP 200 verification
└── lab/                                 # Isolated local test environment
    ├── Dockerfile.node                  # Clean systemd-enabled Ubuntu 22.04 target node
    ├── Dockerfile.control               # Ansible control node with ansible-core
    ├── docker-compose.yml               # Multi-node network topology
    └── lab.ps1                          # PowerShell orchestration automation
```

---

## 4. Execution Evidence: Clean Node Provisioning & First Execution

### 4.1 Clean Target Node Baseline Verification
Prior to provisioning, the target node `rent-node` was checked to ensure no software or state existed:
```bash
docker exec rent-node sh -c "which java || echo 'No Java'; id rentportal || echo 'No app user'; ls -d /opt/rent-portal || echo 'No app dir'"
```
**Output:**
```
No Java
id: 'rentportal': no such user
No app user
ls: cannot access '/opt/rent-portal': No such file or directory
No app dir
```

### 4.2 First Execution Log Summary (`01-first-execution.log`)
Command executed:
```bash
ansible-playbook site.yml -e release_version=1.0.0
```

**Key Execution Tasks:**
- `TASK [prerequisites : Install required OS packages]` $\rightarrow$ **changed** (Installed `openjdk-17-jre-headless`, `curl`, `sqlite3`, `ca-certificates`, `logrotate`)
- `TASK [prerequisites : Create application group]` $\rightarrow$ **changed** (Group `rentportal` created)
- `TASK [prerequisites : Create application service account (no login shell)]` $\rightarrow$ **changed** (User `rentportal` created)
- `TASK [prerequisites : Create application directories]` $\rightarrow$ **changed** (Created `/opt/rent-portal`, `/opt/rent-portal/releases`, `/var/lib/rent-portal/data`, `/var/log/rent-portal`, `/etc/rent-portal`)
- `TASK [prerequisites : Render environment file (runtime configuration)]` $\rightarrow$ **changed** (Rendered `/etc/rent-portal/rent-portal.env`)
- `TASK [prerequisites : Install systemd unit]` $\rightarrow$ **changed** (Installed `/etc/systemd/system/rent-portal.service`)
- `TASK [portal_release : Upload application WAR]` $\rightarrow$ **changed** (Copied `rent-reminder-portal.war` to `/opt/rent-portal/releases/1.0.0/app.war`)
- `TASK [portal_release : Point current at the new release]` $\rightarrow$ **changed** (`/opt/rent-portal/current` linked)
- `RUNNING HANDLER [prerequisites : Restart rent-portal]` $\rightarrow$ **changed** (Service started by systemd)
- `TASK [portal_release : Wait for application port 8081]` $\rightarrow$ **ok** (Port active)
- `TASK [portal_release : HTTP health probe http://127.0.0.1:8081/login]` $\rightarrow$ **ok** (Returned HTTP 200)

**Play Recap:**
```
PLAY RECAP *********************************************************************
rent-node                  : ok=25   changed=11   unreachable=0    failed=0    skipped=5    rescued=0    ignored=0
```
Full log saved in: [`ansible/logs/01-first-execution.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/01-first-execution.log)

---

## 5. Execution Evidence: Idempotency Demonstration

### 5.1 Idempotency Re-run Log Summary (`02-idempotency.log`)
To prove that the configuration script is completely idempotent, `site.yml` was executed a second time against the running node without changing inputs.

Command executed:
```bash
ansible-playbook site.yml -e release_version=1.0.0
```

**Key Execution Tasks:**
- `TASK [prerequisites : Install required OS packages]` $\rightarrow$ **ok**
- `TASK [prerequisites : Create application group]` $\rightarrow$ **ok**
- `TASK [prerequisites : Create application service account]` $\rightarrow$ **ok**
- `TASK [prerequisites : Create application directories]` $\rightarrow$ **ok** (All 5 directories verified)
- `TASK [prerequisites : Render environment file]` $\rightarrow$ **ok** (Unchanged)
- `TASK [prerequisites : Install systemd unit]` $\rightarrow$ **ok** (Unchanged)
- `TASK [portal_release : Upload application WAR]` $\rightarrow$ **ok** (Checksum identical, skipped upload)
- `TASK [portal_release : Point current at the new release]` $\rightarrow$ **ok** (Symlink already targets `1.0.0`)
- `TASK [portal_release : Ensure service is enabled and running]` $\rightarrow$ **ok** (Service already active)
- `TASK [portal_release : HTTP health probe]` $\rightarrow$ **ok** (Returned HTTP 200)

**Play Recap:**
```
PLAY RECAP *********************************************************************
rent-node                  : ok=23   changed=0    unreachable=0    failed=0    skipped=5    rescued=0    ignored=0
```
> [!NOTE]
> `changed=0` and `failed=0` confirms that repeated runs perform zero unnecessary modifications, preserve live state, and maintain deterministic compliance.

Full log saved in: [`ansible/logs/02-idempotency.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/02-idempotency.log)

---

## 6. Execution Evidence: Multi-Vector Health-Check Results

A dedicated health-check playbook (`healthcheck.yml`) evaluates the operational integrity across 6 distinct vector checks.

Command executed:
```bash
ansible-playbook healthcheck.yml
```

### Health Check Report Output:
```yaml
TASK [Health report] ***********************************************************
ok: [rent-node] => {
    "msg": [
        "service        : active / enabled",
        "GET /login     : HTTP 200",
        "GET /tenants   : HTTP 302 -> http://127.0.0.1:8081/login (anonymous redirected)",
        "database       : /var/lib/rent-portal/data/rentdb.sqlite (24576 bytes)",
        "runs as        : rentportal",
        "live release   : /opt/rent-portal/releases/1.0.0",
        "RESULT         : HEALTHY"
    ]
}

PLAY RECAP *********************************************************************
rent-node                  : ok=12   changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```

### Verification Vectors Analyzed:
1. **Service Liveness:** `systemctl is-active rent-portal` returned `active`.
2. **Boot Persistence:** `systemctl is-enabled rent-portal` returned `enabled`.
3. **Public Endpoint Health:** `GET /login` returned `HTTP 200 OK`.
4. **Security Filter Boundary:** `GET /tenants` (protected owner route) returned `HTTP 302 Redirect` to `/login`, proving Spring Security filter chain is actively enforcing authentication.
5. **Database Persistence:** Confirmed `/var/lib/rent-portal/data/rentdb.sqlite` exists and is initialized.
6. **Execution Identity:** Confirmed Java PID runs under non-root system account `rentportal`.

Full log saved in: [`ansible/logs/03-healthcheck.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/03-healthcheck.log)

---

## 7. Execution Evidence: Upgrade, Automated Failure Trapping & Rollback Demonstration

### 7.1 Progressive Upgrade to Release 1.0.1 (`04-deploy-v101.log`)
Command executed:
```bash
ansible-playbook site.yml -e release_version=1.0.1
```
- Created `/opt/rent-portal/releases/1.0.1/app.war`.
- Updated `current` $\rightarrow$ `/opt/rent-portal/releases/1.0.1`.
- Stored previous pointer: `previous` $\rightarrow$ `/opt/rent-portal/releases/1.0.0`.
- Service restarted and passed health check.
- **Result:** `ok=25 changed=5 failed=0`.  
Full log: [`ansible/logs/04-deploy-v101.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/04-deploy-v101.log)

### 7.2 Automated Failure Trapping and Self-Healing Rollback (`05-failed-deploy-auto-rollback.log`)
To test failure recovery, a defective release artifact (`broken-release.war`) was deployed as version `1.0.2`.

**Sequence of Automated Actions:**
1. Directory `/opt/rent-portal/releases/1.0.2` created and corrupted artifact uploaded.
2. Symlink `current` pointed to defective version `1.0.2`.
3. Service restarted.
4. **Failure Detection:** `TASK [portal_release : Wait for application port 8081]` failed with timeout.
5. **Rescue Block Triggered:**
   - Restored `current` symlink back to `/opt/rent-portal/releases/1.0.1`.
   - Restarted `rent-portal` service under verified release `1.0.1`.
   - Executed health check verifying release `1.0.1` responded with `HTTP 200`.
   - Purged the broken directory `/opt/rent-portal/releases/1.0.2`.
   - Failed the deployment play with diagnostic notification:  
     `"Release 1.0.2 FAILED its health check. Rolled back to /opt/rent-portal/releases/1.0.1 and verified healthy."`

**Play Recap:**
```
PLAY RECAP *********************************************************************
rent-node                  : ok=24   changed=7    unreachable=0    failed=1    skipped=3    rescued=1    ignored=0
```
Full log: [`ansible/logs/05-failed-deploy-auto-rollback.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/05-failed-deploy-auto-rollback.log)

### 7.3 Manual / Explicit Rollback to Previous Stable Release (`06-rollback-recovery.log`)
The operational team can also invoke atomic rollback on demand using `rollback.yml`.

Command executed:
```bash
ansible-playbook rollback.yml
```

**Task Results:**
- `TASK [Read release pointers]` $\rightarrow$ Detected `current=1.0.1`, `previous=1.0.0`.
- `TASK [Switch current to /opt/rent-portal/releases/1.0.0]` $\rightarrow$ **changed**.
- `TASK [Keep the rolled-back-from release as previous (allows roll-forward)]` $\rightarrow$ **changed**.
- `TASK [Restart service on rolled-back release]` $\rightarrow$ **changed**.
- `TASK [portal_release : Wait for application port 8081]` $\rightarrow$ **ok**.
- `TASK [portal_release : HTTP health probe http://127.0.0.1:8081/login]` $\rightarrow$ **ok** (HTTP 200).
- `TASK [Rollback summary]` $\rightarrow$ `"Rolled back /opt/rent-portal/releases/1.0.1 -> /opt/rent-portal/releases/1.0.0 and health check passed"`.

**Play Recap:**
```
PLAY RECAP *********************************************************************
rent-node                  : ok=12   changed=3    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```
Full log: [`ansible/logs/06-rollback-recovery.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/06-rollback-recovery.log)

### 7.4 Post-Rollback Node Validation (`07-post-rollback-healthcheck.log`)
Re-running `healthcheck.yml` validates that release `1.0.0` is active and healthy:
```yaml
TASK [Health report] ***********************************************************
ok: [rent-node] => {
    "msg": [
        "service        : active / enabled",
        "GET /login     : HTTP 200",
        "GET /tenants   : HTTP 302 -> http://127.0.0.1:8081/login (anonymous redirected)",
        "database       : /var/lib/rent-portal/data/rentdb.sqlite (24576 bytes)",
        "runs as        : rentportal",
        "live release   : /opt/rent-portal/releases/1.0.0",
        "RESULT         : HEALTHY"
    ]
}
```
Full log: [`ansible/logs/07-post-rollback-healthcheck.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/07-post-rollback-healthcheck.log)

---

## 8. Deliverable Artifacts Index

| Deliverable Item | Location / Evidence Reference | Verification Status |
| :--- | :--- | :--- |
| **Configuration Specification** | Section 2 of this document (`packages, users, folders, files, ports, services`) | Verified complete |
| **Ansible Inventory & Playbooks** | [`ansible/ansible.cfg`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/ansible.cfg), [`ansible/site.yml`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/site.yml), [`ansible/rollback.yml`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/rollback.yml), [`ansible/healthcheck.yml`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/healthcheck.yml), [`ansible/roles/`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/roles) | Tested & validated |
| **First Execution Log** | [`ansible/logs/01-first-execution.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/01-first-execution.log) | Clean node provisioned (`ok=25 changed=11 failed=0`) |
| **Provisioned Target Node** | Dockerized systemd Ubuntu 22.04 node (`rent-node`) with full stack | Live & healthy |
| **Idempotency Evidence** | [`ansible/logs/02-idempotency.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/02-idempotency.log) | Deterministic (`ok=23 changed=0 failed=0`) |
| **Health-Check Result** | [`ansible/logs/03-healthcheck.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/03-healthcheck.log), [`07-post-rollback-healthcheck.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/07-post-rollback-healthcheck.log) | 6/6 vectors passed (`RESULT: HEALTHY`) |
| **Rollback / Recovery Demonstration** | [`ansible/logs/05-failed-deploy-auto-rollback.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/05-failed-deploy-auto-rollback.log), [`ansible/logs/06-rollback-recovery.log`](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/ansible/logs/06-rollback-recovery.log) | Both automated self-healing & manual rollback verified |

---

## 9. Operator Quick-Start Reference

To execute or re-test the automation suite:

```powershell
# 1. Start the lab environment (or reset to a clean state)
.\ansible\lab\lab.ps1 up
# or: .\ansible\lab\lab.ps1 reset

# 2. Run master provisioning and deployment (Release 1.0.0)
docker exec -w /ansible rent-control ansible-playbook site.yml -e release_version=1.0.0

# 3. Test idempotency (should report changed=0)
docker exec -w /ansible rent-control ansible-playbook site.yml -e release_version=1.0.0

# 4. Perform health check verification
docker exec -w /ansible rent-control ansible-playbook healthcheck.yml

# 5. Deploy release 1.0.1
docker exec -w /ansible rent-control ansible-playbook site.yml -e release_version=1.0.1

# 6. Test on-demand rollback
docker exec -w /ansible rent-control ansible-playbook rollback.yml

# 7. Access in web browser
# http://localhost:8082/login (Owner credentials: owner@rentportal.com / owner123)
```
