pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3'
    }

    parameters {
        choice(name: 'DEPLOY_TARGET', choices: ['docker', 'tomcat', 'both'], description: 'Deployment platform (Docker container, Tomcat WAR, or both)')
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: 'Environment profile (dev, staging, prod)')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'rent-reminder-portal', description: 'Docker image repository name')
        string(name: 'DOCKER_TAG_VERSION', defaultValue: '', description: 'Custom version tag (leave empty to use Jenkins BUILD_NUMBER)')
        string(name: 'CONTAINER_PORT', defaultValue: '8081', description: 'Host port to bind the container')
        booleanParam(name: 'PUSH_TO_REGISTRY', defaultValue: false, description: 'Publish image to Docker Hub or Registry')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'docker.io', description: 'Docker registry URL (e.g., docker.io or localhost:5000)')
        string(name: 'DOCKER_REGISTRY_USER', defaultValue: '', description: 'Docker Hub username or registry namespace (e.g., your-dockerhub-username)')
        string(name: 'DOCKER_CREDENTIALS_ID', defaultValue: 'docker-hub-credentials', description: 'Jenkins Credentials ID for registry login')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8080/manager/text', description: 'Tomcat Manager API URL (used when DEPLOY_TARGET is tomcat/both)')
    }

    environment {
        SPRING_PROFILES_ACTIVE = "${params.DEPLOY_ENV}"
        IMAGE_NAME = "${params.DOCKER_IMAGE_NAME}"
        CONTAINER_NAME = "rent-portal-app"
        HOST_PORT = "${params.CONTAINER_PORT}"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out the source code...'
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Backwardbus03/Jenkins-mini-project.git']])
            }
        }

        stage('Compile') {
            steps {
                echo "Compiling the application for environment: ${params.DEPLOY_ENV}..."
                bat 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Executing Selenium Automated Regression Suite...'
                // If any test fails, Maven exits with non-zero code, halting pipeline before Package, Docker Build and Deploy
                bat 'mvn test'
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application into a WAR file...'
                bat 'mvn package -DskipTests'
            }
        }

        stage('Docker Build & Tag') {
            when {
                expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
            }
            steps {
                script {
                    env.EFFECTIVE_TAG = params.DOCKER_TAG_VERSION?.trim() ? params.DOCKER_TAG_VERSION.trim() : env.BUILD_NUMBER
                    echo "Building Docker image: ${IMAGE_NAME}:${env.EFFECTIVE_TAG} and ${IMAGE_NAME}:latest..."
                }
                bat '''
                    @echo off
                    echo Building versioned Docker image: %IMAGE_NAME%:%EFFECTIVE_TAG%...
                    if exist Dockerfile.local (
                        docker build -f Dockerfile.local -t %IMAGE_NAME%:%EFFECTIVE_TAG% -t %IMAGE_NAME%:latest .
                    ) else (
                        docker build -t %IMAGE_NAME%:%EFFECTIVE_TAG% -t %IMAGE_NAME%:latest .
                    )
                    echo Listing generated Docker images:
                    docker images %IMAGE_NAME%
                '''
            }
        }

        stage('Docker Publish') {
            when {
                allOf {
                    expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
                    expression { return params.PUSH_TO_REGISTRY.toBoolean() }
                }
            }
            steps {
                script {
                    def registryUser = params.DOCKER_REGISTRY_USER?.trim()
                    def registryHost = params.DOCKER_REGISTRY?.trim() ?: 'docker.io'

                    if (registryUser) {
                        def fullRepo = "${registryUser}/${IMAGE_NAME}"
                        echo "Tagging and publishing image to ${registryHost}/${fullRepo}..."

                        bat """
                            docker tag ${IMAGE_NAME}:${env.EFFECTIVE_TAG} ${fullRepo}:${env.EFFECTIVE_TAG}
                            docker tag ${IMAGE_NAME}:latest ${fullRepo}:latest
                        """

                        try {
                            withCredentials([usernamePassword(credentialsId: params.DOCKER_CREDENTIALS_ID, usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                                bat """
                                    @echo off
                                    echo %REG_PASS% | docker login ${registryHost} -u %REG_USER% --password-stdin
                                    docker push ${fullRepo}:${env.EFFECTIVE_TAG}
                                    docker push ${fullRepo}:latest
                                    docker logout ${registryHost}
                                """
                            }
                            echo "Successfully pushed ${fullRepo}:${env.EFFECTIVE_TAG} to ${registryHost}."
                        } catch (Exception e) {
                            echo "Warning: Credential login failed or ID '${params.DOCKER_CREDENTIALS_ID}' not configured. Attempting direct push..."
                            bat """
                                docker push ${fullRepo}:${env.EFFECTIVE_TAG}
                                docker push ${fullRepo}:latest
                            """
                        }
                    } else if (registryHost && registryHost != 'docker.io') {
                        def targetRepo = "${registryHost}/${IMAGE_NAME}"
                        echo "Publishing to local/private registry ${targetRepo}..."
                        bat """
                            docker tag ${IMAGE_NAME}:${env.EFFECTIVE_TAG} ${targetRepo}:${env.EFFECTIVE_TAG}
                            docker tag ${IMAGE_NAME}:latest ${targetRepo}:latest
                            docker push ${targetRepo}:${env.EFFECTIVE_TAG}
                            docker push ${targetRepo}:latest
                        """
                    } else {
                        echo "No registry username or private registry host provided. Skipping publish."
                    }
                }
            }
        }

        stage('Docker Deploy') {
            when {
                expression { return params.DEPLOY_TARGET == 'docker' || params.DEPLOY_TARGET == 'both' }
            }
            steps {
                echo "Deploying fresh container: ${CONTAINER_NAME} using image ${IMAGE_NAME}:${env.EFFECTIVE_TAG} on port ${HOST_PORT}..."
                bat '''
                    @echo off
                    echo Stopping existing container '%CONTAINER_NAME%' if running...
                    docker stop %CONTAINER_NAME% >nul 2>&1 || ver >nul
                    echo Removing old container '%CONTAINER_NAME%'...
                    docker rm -f %CONTAINER_NAME% >nul 2>&1 || ver >nul

                    echo Starting fresh container '%CONTAINER_NAME%'...
                    docker run -d --name %CONTAINER_NAME% -p %HOST_PORT%:8081 -v rent_data:/app/data --restart unless-stopped %IMAGE_NAME%:%EFFECTIVE_TAG%

                    echo Verifying active container:
                    docker ps --filter "name=%CONTAINER_NAME%"
                '''
                sleep time: 5, unit: 'SECONDS'
                bat '''
                    @echo off
                    echo Inspecting recent container logs:
                    docker logs --tail 25 %CONTAINER_NAME%
                    echo Checking container HTTP accessibility:
                    curl -I http://localhost:%HOST_PORT%/login
                    ver >nul
                '''
            }
        }

        stage('Tomcat Deploy') {
            when {
                expression { return params.DEPLOY_TARGET == 'tomcat' || params.DEPLOY_TARGET == 'both' }
            }
            steps {
                echo "Deploying to Tomcat server at ${params.TOMCAT_URL}..."
                bat '''
                    copy /Y target\\rent-reminder-portal.war "C:\\DevTools\\apache-tomcat\\apache-tomcat-11.0.24\\webapps\\rent-reminder-portal.war"
                '''
            }
        }
    }

    post {
        always {
            echo 'Publishing test reports and archiving build artifacts...'
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/*.war, target/screenshots/*.png, target/selenium-reports/**', allowEmptyArchive: true, fingerprint: true
        }
        success {
            echo 'Continuous Testing, Packaging, and Automated Docker Deployment Successful!'
        }
        failure {
            echo 'Pipeline Failed: Deployment halted due to test failure, compilation error, or container deployment issue.'
        }
    }
}
