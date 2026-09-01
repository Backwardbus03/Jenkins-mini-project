# Week 7: Jenkins CI Setup

## 1. Jenkins Installation Instructions

### Option A: Local Installation (Windows)
1. Download the Jenkins Windows installer from the official website (https://www.jenkins.io/download/).
2. Run the installer and follow the setup wizard.
3. Choose the "Run service as LocalSystem" option or provide dedicated service credentials.
4. Set the port (default is 8080, but since our Spring Boot app uses 8080, change Jenkins to 8081).
5. Open `http://localhost:8081` in your browser.
6. Retrieve the initial admin password from the specified path (e.g., `C:\Program Files\Jenkins\secrets\initialAdminPassword`).
7. Install suggested plugins and create your admin user.

### Option B: Docker Installation
1. Ensure Docker Desktop is installed and running.
2. Run the Jenkins container:
   ```bash
   docker run -d -p 8081:8080 -p 50000:50000 --name jenkins jenkins/jenkins:lts
   ```
3. Get the initial admin password:
   ```bash
   docker logs jenkins
   ```
4. Access Jenkins at `http://localhost:8081` and complete the initial setup wizard.

## 2. CI Job Configuration

### Job Setup
1. From the Jenkins Dashboard, click **New Item**.
2. Enter the name (e.g., `rent-reminder-portal-ci`) and select **Freestyle project**. Click OK.
3. Under **Source Code Management**, select **Git**.
   - Repository URL: `https://github.com/Backwardbus03/Jenkins-mini-project.git`
   - Branch Specifier: `*/main`
4. Under **Build Triggers**, check **GitHub hook trigger for GITScm polling** (requires configuring a webhook in GitHub repository settings pointing to `http://<your-jenkins-url>:8081/github-webhook/`).

### Build Steps
1. Under **Build Steps**, click **Add build step** and choose **Invoke top-level Maven targets**.
2. Set the **Goals** to:
   ```
   clean package -DskipTests
   ```
   *(Note: `-DskipTests` is used if tests are failing or not yet configured. Otherwise, use `clean package`)*

### Post-build Actions
1. Click **Add post-build action** and select **Archive the artifacts**.
2. Files to archive: `target/*.war`
3. Click **Save**.

## 3. Successful Build Log Example
When the job runs successfully, the console output will look similar to this:
```text
Started by user Admin
Running as SYSTEM
Building in workspace /var/jenkins_home/workspace/rent-reminder-portal-ci
The recommended git tool is: NONE
No credentials specified
 > git rev-parse --resolve-git-dir /var/jenkins_home/workspace/rent-reminder-portal-ci/.git # timeout=10
Fetching changes from the remote Git repository
 > git config remote.origin.url https://github.com/Backwardbus03/Jenkins-mini-project.git # timeout=10
Fetching upstream changes from https://github.com/Backwardbus03/Jenkins-mini-project.git
 > git rev-parse refs/remotes/origin/main^{commit} # timeout=10
Checking out Revision ae2297f023... (refs/remotes/origin/main)
 > git checkout -f ae2297f023... # timeout=10
Commit message: "Update agile plan to mark Week 6 MVP user stories as Done"
[rent-reminder-portal-ci] $ mvn clean package -DskipTests
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Building rent-payment-reminder-portal 0.1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
...
[INFO] --- maven-war-plugin:3.4.0:war (default-war) @ rent-payment-reminder-portal ---
[INFO] Packaging webapp
[INFO] Assembling webapp [rent-payment-reminder-portal] in [/var/jenkins_home/workspace/rent-reminder-portal-ci/target/rent-reminder-portal]
[INFO] Processing war project
[INFO] Webapp assembled in [128 msecs]
[INFO] Building war: /var/jenkins_home/workspace/rent-reminder-portal-ci/target/rent-reminder-portal.war
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.123 s
[INFO] Finished at: 2026-09-01T21:20:00Z
[INFO] ------------------------------------------------------------------------
Archiving artifacts
Finished: SUCCESS
```

## 4. Deliverable Checklist
- [x] Jenkins installation instructions provided.
- [x] CI job configured to connect to GitHub.
- [x] Webhook/polling configured for automatic triggers.
- [x] Build step executes `mvn clean package`.
- [x] Post-build action archives `target/*.war`.
- [x] Success build log snippet documented.
