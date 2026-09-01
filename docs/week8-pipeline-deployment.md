# Week 8: Pipeline as Code and Server Deployment

This document explains the Jenkins pipeline setup using a declarative `Jenkinsfile` and how to verify the deployment.

## 1. Jenkinsfile Stages Explanation

The `Jenkinsfile` located at the root of the repository defines our CI/CD pipeline. It includes the following elements:

### Parameters & Environment
- **Parameters**: 
  - `DEPLOY_ENV`: A choice parameter allowing you to select the target environment (`dev`, `staging`, `prod`).
  - `TOMCAT_URL`: A string parameter defining where the Tomcat Manager API is located.
- **Environment**: Sets the `SPRING_PROFILES_ACTIVE` variable based on the selected `DEPLOY_ENV`.

### Stages
1. **Checkout**: 
   - Uses the `checkout scmGit` plugin step to pull the latest source code from the `main` branch of our GitHub repository.
2. **Build**: 
   - Executes `mvn clean compile` to compile the Java source code and verify that there are no syntax or compilation errors.
3. **Package**: 
   - Executes `mvn package -DskipTests` to bundle the compiled application into a deployable `.war` artifact. Tests are skipped in this stage to speed up the basic deployment demo.
4. **Deploy**: 
   - Uses a script to copy the generated WAR file (`target\rent-reminder-portal.war`) to the Tomcat `webapps` directory. If Tomcat is running, it will automatically hot-deploy the WAR file.

### Post-build Actions
- **always**: Archives the generated WAR file so it can be downloaded directly from the Jenkins build page.
- **success/failure**: Prints the corresponding deployment status to the console output.

## 2. How to Run the Pipeline

1. In Jenkins, create a new **Pipeline** job (e.g., `rent-reminder-pipeline`).
2. Under **Pipeline**, set "Definition" to **Pipeline script from SCM**.
3. Select **Git** as the SCM and enter the repository URL: `https://github.com/Backwardbus03/Jenkins-mini-project.git`.
4. Ensure the **Branch Specifier** is `*/main` and the **Script Path** is `Jenkinsfile`.
5. Save the job.
6. Click **Build with Parameters** on the left menu.
7. Select the desired `DEPLOY_ENV` and verify the `TOMCAT_URL`.
8. Click **Build**.

## 3. How to Verify Deployment

1. Once the pipeline finishes successfully, open your web browser.
2. Navigate to your local Tomcat server URL and include the application context path, e.g.:
   `http://localhost:8080/rent-reminder-portal/tenants`
3. If the page loads and shows the "Tenants" list view, the deployment was successful.
4. You can also log into the Tomcat Manager App (`http://localhost:8080/manager/html`) to verify that the `rent-reminder-portal` application is listed and its status is `running`.

## 4. Deliverable Checklist
- [x] `Jenkinsfile` created at the repository root.
- [x] Pipeline includes Checkout, Build, Package, and Deploy stages.
- [x] Pipeline uses parameters (`DEPLOY_ENV`, `TOMCAT_URL`).
- [x] `week8-pipeline-deployment.md` documentation created explaining stages and execution.
