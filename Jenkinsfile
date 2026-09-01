pipeline {
    agent any

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: 'Select the environment to deploy to')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8080/manager/text', description: 'Tomcat Manager API URL')
    }

    environment {
        // Environment variables can be defined here based on the DEPLOY_ENV parameter
        SPRING_PROFILES_ACTIVE = "${params.DEPLOY_ENV}"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out the source code from GitHub...'
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Backwardbus03/Jenkins-mini-project.git']])
            }
        }

        stage('Build') {
            steps {
                echo "Building the application for environment: ${params.DEPLOY_ENV}..."
                // Use Maven to compile the project
                bat 'mvn clean compile'
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application into a WAR file...'
                // Skip tests for rapid deployment demo, normally you'd run them
                bat 'mvn package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying to Tomcat server at ${params.TOMCAT_URL}..."
                // Deploy via tomcat manager using curl or copy to local webapps if local.
                // Assuming local tomcat instance for this mini-project demo:
                bat 'copy target\\rent-reminder-portal.war C:\\apache-tomcat\\webapps\\rent-reminder-portal.war'
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished.'
            archiveArtifacts artifacts: 'target/*.war', fingerprint: true
        }
        success {
            echo 'Deployment Successful!'
        }
        failure {
            echo 'Deployment Failed.'
        }
    }
}
