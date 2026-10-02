# Jenkins Docker Integration: Automated Build, Versioning, Registry Publishing & Container Deployment

This document details the automated **CI/CD containerization pipeline** for the **Rent Payment Reminder Portal**, extending Jenkins Declarative Pipeline to build versioned Docker images, publish artifacts to Docker Hub or a local registry, and automatically deploy fresh containers upon passing automated regression tests.

---

## 1. Overview & Pipeline Architecture

Continuous Integration and Continuous Deployment (CI/CD) with Docker guarantees that tested code is packaged into immutable, standardized containers and deployed automatically without manual intervention.

```
+------------------------------------------------------------------------------------------------------------------+
|                                           Jenkins CI/CD Automation Flow                                          |
|                                                                                                                  |
|  +--------------+       +-------------+       +-------------------+       +---------------+                      |
|  |   Checkout   | ----> |   Compile   | ----> |  Test (Selenium)  | ----> |    Package    |                      |
|  | (Git Source) |       | (mvn clean) |       |  [Quality Gate]   |       | (mvn package) |                      |
|  +--------------+       +-------------+       +---------+---------+       +-------+-------+                      |
|                                                         |                         |                              |
|                                                    Tests Fail?                    v                              |
|                                                    (HALT BUILD)       +-----------------------+                  |
|                                                                       |  Docker Build & Tag   |                  |
|                                                                       | (image:${BUILD_NUM})  |                  |
|                                                                       +-----------+-----------+                  |
|                                                                                   |                              |
|                                      +--------------------------------------------+                              |
|                                      |                                                                           |
|                                      v (Optional: PUSH_TO_REGISTRY=true)                                         |
|                         +--------------------------+                                                             |
|                         |      Docker Publish      |                                                             |
|                         | (Docker Hub / Registry)  |                                                             |
|                         +------------+-------------+                                                             |
|                                      |                                                                           |
|                                      v                                                                           |
|                         +--------------------------+                                                             |
|                         |      Docker Deploy       |                                                             |
|                         | (Stop old -> Run fresh)  |                                                             |
|                         +------------+-------------+                                                             |
|                                      |                                                                           |
|                                      v                                                                           |
|                         +--------------------------+                                                             |
|                         |  Post: JUnit & Health    |                                                             |
|                         | (Publish test & logs)    |                                                             |
|                         +--------------------------+                                                             |
+------------------------------------------------------------------------------------------------------------------+
```

### Key Architectural Highlights
1. **Automated Quality Gate**: The pipeline runs the full Selenium automated UI regression suite (`mvn test`) before triggering any Docker image builds. If an assertion fails, the pipeline immediately halts, ensuring defective builds are never packaged or deployed.
2. **Dynamic Semantic Versioning**: Docker images are automatically tagged with the unique Jenkins `${BUILD_NUMBER}` (e.g., `rent-reminder-portal:14`), alongside the `:latest` pointer for traceability.
3. **Flexible Publishing**: Built-in support for Docker Hub authentication via Jenkins Credential Store (`docker-hub-credentials`) or local unauthenticated registries (e.g. `localhost:5000`).
4. **Automated Fresh Deployment**:
   - Safely terminates and removes the existing container instance (`rent-portal-app`).
   - Launches a fresh container mapped to host port `8081`.
   - Mounts a persistent Docker named volume (`rent_data:/app/data`) preserving the SQLite database (`rentdb.sqlite`) across container rebuilds.
   - Executes automated HTTP probing to confirm healthy startup.

---

## 2. Pipeline Parameters Reference

The pipeline is parameterized via the Declarative `parameters` directive:

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `DEPLOY_TARGET` | Choice | `docker` | Target platform (`docker`, `tomcat`, or `both`) |
| `DEPLOY_ENV` | Choice | `dev` | Environment profile (`dev`, `staging`, `prod`) |
| `DOCKER_IMAGE_NAME` | String | `rent-reminder-portal` | Docker image repository name |
| `DOCKER_TAG_VERSION` | String | *(Empty)* | Custom tag (leaves blank to use Jenkins `${BUILD_NUMBER}`) |
| `CONTAINER_PORT` | String | `8081` | Host port to bind the application container |
| `PUSH_TO_REGISTRY` | Boolean | `false` | Enable/disable publishing to Docker Hub or registry |
| `DOCKER_REGISTRY` | String | `docker.io` | Registry host URL (e.g., `docker.io` or `localhost:5000`) |
| `DOCKER_REGISTRY_USER`| String | *(Empty)* | Docker Hub username or namespace |
| `DOCKER_CREDENTIALS_ID`| String | `docker-hub-credentials`| Jenkins Credential ID containing username and password/token |
| `TOMCAT_URL` | String | `http://localhost:8080/manager/text` | Tomcat Manager URL (used if `DEPLOY_TARGET` includes Tomcat) |

---

## 3. Jenkinsfile Stages Deep Dive

### Stage 1: Checkout
Pulls the latest revision of the application source code from the GitHub `main` branch.

### Stage 2: Compile
Executes `mvn clean compile` to validate Java syntax and ensure all dependencies are resolved.

### Stage 3: Test (Selenium Automated Regression Quality Gate)
Executes `mvn test`. Headless Chrome tests verify tenant management, payment reminders, and navigation flows.
- **Fail-Fast Policy**: If any test scenario fails, Maven returns a non-zero exit code, immediately aborting the pipeline before packaging or deployment.

### Stage 4: Package
Runs `mvn package -DskipTests` to assemble the deployable Spring Boot WAR (`target/rent-reminder-portal.war`).

### Stage 5: Docker Build & Tag
Computes the dynamic version tag:
- If `DOCKER_TAG_VERSION` is specified by the user, that version is used.
- Otherwise, it defaults to the unique Jenkins `${BUILD_NUMBER}` (e.g., `14`).

Builds the image using `Dockerfile.local` (or standard `Dockerfile`):
```bat
docker build -f Dockerfile.local -t rent-reminder-portal:%EFFECTIVE_TAG% -t rent-reminder-portal:latest .
```

### Stage 6: Docker Publish (Conditional)
Executes only when `PUSH_TO_REGISTRY` is `true`.
- Tags images with the target repository prefix (e.g., `myusername/rent-reminder-portal:14`).
- Securely pulls Docker credentials using `withCredentials([usernamePassword(...)])`.
- Pushes both the versioned tag and `:latest` to Docker Hub or a local registry:
```bat
docker push %REGISTRY_USER%/rent-reminder-portal:%EFFECTIVE_TAG%
docker push %REGISTRY_USER%/rent-reminder-portal:latest
```

### Stage 7: Docker Deploy (Fresh Container Lifecycle)
Automates container replacement:
1. Gracefully stops any currently running container:
   ```bat
   docker stop rent-portal-app >nul 2>&1 || ver >nul
   ```
2. Removes the old container:
   ```bat
   docker rm -f rent-portal-app >nul 2>&1 || ver >nul
   ```
3. Runs the fresh versioned container:
   ```bat
   docker run -d --name rent-portal-app -p 8081:8081 -v rent_data:/app/data --restart unless-stopped rent-reminder-portal:%EFFECTIVE_TAG%
   ```
4. Verifies container health and HTTP responsiveness:
   ```bat
   curl -s -o nul -w "HTTP Response Code: %{http_code}\n" http://localhost:8081/login
   ```

### Stage 8: Post Actions
- **JUnit Test Archival**: Parses `target/surefire-reports/*.xml` to display graphical test trends.
- **Artifact Archiving**: Archives the WAR file, failure screenshots, and test logs.

---

## 4. How to Configure and Run the Pipeline

### Step 1: Ensure Host Prerequisites are Running
1. **Docker Desktop**: Must be started and running on the host machine (`docker info`).
2. **Jenkins**: Running on `http://localhost:8080`.

### Step 2: (Optional) Add Docker Hub Credentials in Jenkins
If you want to push images to Docker Hub:
1. Navigate to **Jenkins** $\rightarrow$ **Manage Jenkins** $\rightarrow$ **Credentials** $\rightarrow$ **System** $\rightarrow$ **Global credentials**.
2. Click **Add Credentials**.
3. Select **Username with password**:
   - **Username**: Your Docker Hub username.
   - **Password**: Your Docker Hub Personal Access Token (or password).
   - **ID**: `docker-hub-credentials`.
4. Click **Create**.

### Step 3: Trigger the Pipeline Run
1. Go to **Jenkins Dashboard** $\rightarrow$ Select your pipeline job (e.g., `rent-reminder-pipeline`).
2. Click **Build with Parameters** in the left navigation menu.
3. Configure the build:
   - **`DEPLOY_TARGET`**: Select `docker`.
   - **`DEPLOY_ENV`**: Select `dev`.
   - **`CONTAINER_PORT`**: `8081`.
   - **`PUSH_TO_REGISTRY`**: Check if you wish to push to Docker Hub; otherwise leave unchecked for local container deployment.
   - **`DOCKER_REGISTRY_USER`**: Your Docker Hub username (if pushing).
4. Click **Build**.

---

## 5. Verification & Running Container Evidence

### 1. View Jenkins Build Console Output
Monitor the Jenkins console output to observe:
- Selenium test execution results.
- Docker build output showing image layers.
- Container stop/rm and `docker run` execution.
- HTTP probe returning `200 OK`.

### 2. Verify Running Container via CLI
On the host machine, run:
```powershell
docker ps --filter "name=rent-portal-app"
```
Sample Output:
```
CONTAINER ID   IMAGE                     COMMAND                  CREATED         STATUS                   PORTS                    NAMES
8b3e1f54a9d2   rent-reminder-portal:14   "sh -c 'java ${JAVA_…"   8 seconds ago   Up 7 seconds (healthy)   0.0.0.0:8081->8081/tcp   rent-portal-app
```

### 3. Check Live Application Logs
```powershell
docker logs --tail 50 rent-portal-app
```
Look for:
```
Tomcat started on port 8081 (http) with context path ''
Started RentReminderApplication in 3.124 seconds
```

### 4. Access Portal in Web Browser
Open: [http://localhost:8081/login](http://localhost:8081/login)  
- **Owner Credentials**: `owner@rentportal.com` / `owner123`
- Confirm tenant listing, payment records, and responsive UI navigation.

---

## 6. Deliverable Checklist
- [x] Extended `Jenkinsfile` with automated `Docker Build & Tag`, `Docker Publish`, and `Docker Deploy` stages.
- [x] Dynamic versioning implemented using Jenkins `${BUILD_NUMBER}`.
- [x] Automated quality gate configured halting builds if Selenium tests fail.
- [x] Secure Docker Hub / registry publishing using Jenkins `withCredentials`.
- [x] Automated fresh container deployment replacing stale containers.
- [x] Persistent volume mount configured (`rent_data:/app/data`) for SQLite database retention.
- [x] `docs/jenkins-docker-integration.md` created with complete architectural diagrams, parameter reference, execution steps, and verification commands.
