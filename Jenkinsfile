pipeline {
    agent any

    tools {
        jdk 'JDK-21'
        maven 'Maven-3'
    }

    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['dev', 'staging', 'prod'], description: 'Select the environment to deploy to')
        string(name: 'TOMCAT_URL', defaultValue: 'http://localhost:8080/manager/text', description: 'Tomcat Manager API URL')
    }

    environment {
        SPRING_PROFILES_ACTIVE = "${params.DEPLOY_ENV}"
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
                // If any test fails, Maven exits with non-zero code, halting pipeline before Package and Deploy
                bat 'mvn test'
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application into a WAR file...'
                bat 'mvn package -DskipTests'
            }
        }

        stage('Deploy') {
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
            echo 'Continuous Testing & Deployment Successful!'
        }
        failure {
            echo 'Pipeline Failed: Deployment halted due to test failure or compilation error.'
        }
    }
}
