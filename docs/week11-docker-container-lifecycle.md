# Week 11: Docker Image and Container Lifecycle

This document provides a comprehensive guide for containerizing the **Rent Payment Reminder Portal** using Docker, including multi-stage image creation, container lifecycle operations, port mapping, log inspection, and persistence configuration.

---

## 1. Overview & Architectural Design

The Rent Payment Reminder Portal is a Spring Boot 3.2.5 web application running on Java 17 with an embedded SQLite persistence store and Thymeleaf UI.

```
+-----------------------------------------------------------------------------------+
|                                 Docker Host                                       |
|                                                                                   |
|   Host Port: 8081  <-------- Port Forwarding (-p 8081:8081) --------+             |
|                                                                     |             |
|   +-------------------------------------------------------------+   |             |
|   | Container: rent-portal-app                                  |   |             |
|   | Base Image: eclipse-temurin:17-jre-jammy                    |   |             |
|   | User: appuser (non-root, UID 1000)                          |   |             |
|   | Port: 8081 (EXPOSE 8081)                                    |---+             |
|   | Process: java -jar /app/app.war                             |                 |
|   |                                                             |                 |
|   |   Data Directory: /app/data/rentdb.sqlite                   |                 |
|   |           ^                                                 |                 |
|   +-----------|-------------------------------------------------+                 |
|               | Mounted Volume (-v rent_data:/app/data)                           |
|   +-----------+-----------+                                                       |
|   | Named Volume:         |                                                       |
|   | rent_data (Docker)    |                                                       |
|   +-----------------------+                                                       |
+-----------------------------------------------------------------------------------+
```

### Dockerfile Design Highlights
1. **Multi-Stage Build (`Dockerfile`)**:
   - **Stage 1 (`builder`)**: Uses `maven:3.9.6-eclipse-temurin-17` to compile and package the WAR artifact from source. Builds are 100% self-contained and reproducible across developer machines and CI runners.
   - **Stage 2 (`runtime`)**: Uses `eclipse-temurin:17-jre-jammy` minimal JRE image. Eliminates Maven and build tools, drastically shrinking image size and attack surface.
2. **Fast Local Build (`Dockerfile.local`)**:
   - For fast local iterations when `target/rent-reminder-portal.war` is already compiled on the host (`mvn clean package -DskipTests`), this Dockerfile builds the container in ~2 seconds.
3. **Container Security Best Practices**:
   - Executes under an unprivileged system user (`appuser:appgroup`), avoiding container execution as `root`.
4. **Data Persistence**:
   - Dedicated `/app/data` directory for `rentdb.sqlite`, allowing seamless Docker Volume mounts (`-v rent_data:/app/data`) so tenant and payment data persists across container restarts and updates.
5. **Container Health Checking**:
   - Integrated `HEALTHCHECK` probing `http://localhost:8081/login` every 30 seconds.

---

## 2. Docker Files Reference

### Primary Multi-Stage Dockerfile (`Dockerfile`)
```dockerfile
# Stage 1: Build Application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Environment
FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="Rent Reminder DevOps Team" app="rent-payment-reminder-portal"
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /workspace/target/rent-reminder-portal.war /app/app.war

USER appuser
EXPOSE 8081

ENV SERVER_PORT=8081 \
    SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/rentdb.sqlite \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/login || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -Dspring.datasource.url=${SPRING_DATASOURCE_URL} -jar /app/app.war"]
```

---

## 3. Step-by-Step Container Lifecycle Execution

### Step 1: Ensure Docker Desktop is Running
Launch Docker Desktop from your Windows Start Menu or Desktop shortcut:
```powershell
docker info
```
Verify that the output displays the active Docker Server version and runtime information.

---

### Step 2: Build and Tag the Docker Image

#### Option A: Full Multi-Stage Build (Compiles inside Docker)
```powershell
docker build -t rent-reminder-portal:1.0 -t rent-reminder-portal:latest .
```

#### Option B: Fast Local Build (Using pre-compiled target WAR)
```powershell
mvn clean package -DskipTests
docker build -f Dockerfile.local -t rent-reminder-portal:1.0 -t rent-reminder-portal:latest .
```

---

### Step 3: Inspect the Docker Image Details
Check image list and metadata:
```powershell
# List images
docker images rent-reminder-portal

# Inspect image details (architecture, environment variables, exposed ports)
docker inspect rent-reminder-portal:1.0
```

---

### Step 4: Run the Container with Port Mapping and Volume Mount
Run the container in detached mode (`-d`), giving it the name `rent-portal-app`, mapping port `8081` on the host to `8081` in the container, and mounting a persistent volume for the SQLite database:
```powershell
docker run -d `
  --name rent-portal-app `
  -p 8081:8081 `
  -v rent_data:/app/data `
  --restart unless-stopped `
  rent-reminder-portal:1.0
```

---

### Step 5: Verify Running Container Status
```powershell
docker ps --filter "name=rent-portal-app"
```
Sample Output:
```
CONTAINER ID   IMAGE                    COMMAND                  CREATED         STATUS                   PORTS                    NAMES
a1b2c3d4e5f6   rent-reminder-portal:1.0 "sh -c 'java ${JAVA_…"   10 seconds ago  Up 9 seconds (healthy)   0.0.0.0:8081->8081/tcp   rent-portal-app
```

---

### Step 6: Inspect Container Logs
Stream real-time application logs from the Spring Boot container:
```powershell
# Follow logs live
docker logs -f rent-portal-app

# View last 100 log lines with timestamps
docker logs --tail 100 -t rent-portal-app
```
Key log milestone to verify:
```
Tomcat started on port 8081 (http) with context path ''
Started RentReminderApplication in 3.421 seconds
```

---

### Step 7: Verify Web Access in Browser / cURL
Open your browser or run curl:
```powershell
# HTTP check from host terminal
curl http://localhost:8081/login -I
```
Web Browser URLs:
- **Landing / Home**: `http://localhost:8081/`
- **Owner Login**: `http://localhost:8081/login` (Default credentials: `owner@rentportal.com` / `owner123`)
- **Tenant Management**: `http://localhost:8081/tenants`

---

### Step 8: Container Health Check & Metrics
```powershell
# Check health status
docker inspect --format='{{json .State.Health.Status}}' rent-portal-app

# Monitor live CPU and Memory usage
docker stats --no-stream rent-portal-app
```

---

### Step 9: Container Lifecycle Operations (Stop, Restart, Pause, Remove)

#### 1. Stop Container (Graceful SIGTERM)
```powershell
docker stop rent-portal-app
# Verify container is stopped
docker ps -a --filter "name=rent-portal-app"
```

#### 2. Start / Restart Container
```powershell
# Start stopped container
docker start rent-portal-app

# Restart running container
docker restart rent-portal-app
```

#### 3. Pause & Unpause Container
```powershell
# Suspend all processes in the container
docker pause rent-portal-app

# Resume processes
docker unpause rent-portal-app
```

#### 4. Remove Container
```powershell
# Stop and remove container
docker stop rent-portal-app
docker rm rent-portal-app

# Or force remove in one command
docker rm -f rent-portal-app
```

#### 5. Remove Docker Image
```powershell
docker rmi rent-reminder-portal:1.0
```

---

## 4. Complete Container Lifecycle Command Matrix

| Operation | Command | Purpose |
| :--- | :--- | :--- |
| **Build & Tag** | `docker build -t rent-reminder-portal:1.0 .` | Builds multi-stage production image |
| **Inspect Image** | `docker inspect rent-reminder-portal:1.0` | Views image layers, ENV, and configuration |
| **Run Container** | `docker run -d --name rent-portal-app -p 8081:8081 rent-reminder-portal:1.0` | Runs detached container with port forwarding |
| **List Running** | `docker ps` | Lists active containers and ports |
| **Inspect Logs** | `docker logs -f rent-portal-app` | Views application startup and request logs |
| **Health Check** | `docker inspect --format='{{json .State.Health}}' rent-portal-app` | Verifies HTTP probe health status |
| **Pause/Unpause** | `docker pause rent-portal-app` / `docker unpause rent-portal-app` | Freezes/resumes container CPU execution |
| **Stop** | `docker stop rent-portal-app` | Sends SIGTERM for graceful shutdown |
| **Restart** | `docker restart rent-portal-app` | Restarts application container |
| **Remove Container** | `docker rm -f rent-portal-app` | Deletes container instance |
| **Remove Image** | `docker rmi rent-reminder-portal:1.0` | Cleans up local Docker image cache |

---

## 5. Deliverable Checklist
- [x] Multi-stage `Dockerfile` created at repository root (`maven:3.9.6` builder + `eclipse-temurin:17-jre-jammy` runtime).
- [x] Fast local build `Dockerfile.local` created for instant build workflows.
- [x] `.dockerignore` configured to exclude unnecessary build artifacts, git history, and temporary files.
- [x] Port mapping documented (`-p 8081:8081`).
- [x] Persistence directory `/app/data` configured for SQLite database (`rentdb.sqlite`).
- [x] Container lifecycle commands documented: build, tag, run, logs, stop, restart, pause, unpause, and remove.
- [x] `docs/week11-docker-container-lifecycle.md` created with architectural diagram and command matrix.

