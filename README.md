# Rent Payment Reminder Portal

Jenkins-based DevOps project for a Rent Payment Reminder Portal (Batch-1, BE CMPN B).

## Features
- Tenant Management
- Payment Tracking
- Continuous Integration & Deployment with Jenkins
- Automated Selenium Regression Testing
- Docker Containerization & Lifecycle Management

## Documentation
- [Week 2: Agile Project Plan](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week2-agile-plan.md)
- [Week 3: System Architecture](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week3-architecture.md)
- [Week 7: Jenkins CI Setup](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week7-jenkins-ci.md)
- [Week 8: Pipeline Deployment](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week8-pipeline-deployment.md)
- [Week 9: Selenium Automated Testing](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week9-selenium-testing.md)
- [Week 10: Continuous Testing in Jenkins](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week10-continuous-testing.md)
- [Week 11: Docker Image and Container Lifecycle](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/week11-docker-container-lifecycle.md)
- [Jenkins Docker Integration: Automated CI/CD Pipeline](file:///c:/Users/sumed/IdeaProjects/rent-reminder-portal/docs/jenkins-docker-integration.md)


## Quick Start with Docker
```bash
# Build Docker image
docker build -t rent-reminder-portal:1.0 .

# Run container with port forwarding and database persistence
docker run -d --name rent-portal-app -p 8081:8081 -v rent_data:/app/data rent-reminder-portal:1.0

# Open in browser: http://localhost:8081
```