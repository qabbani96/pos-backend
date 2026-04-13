pipeline {
    agent any

    environment {
        // Project name used to keep Docker containers organized
        COMPOSE_PROJECT_NAME = "resume_analyzer"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                echo 'Cleaning up previous build files...'
                deleteDir()
            }
        }

        stage('Checkout') {
            steps {
                echo 'Pulling latest code from GitHub...'
                checkout scm
            }
        }

        stage('Build Java App') {
            steps {
                echo 'Compiling Spring Boot application...'
                // Ensures Maven wrapper is executable
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Deploy Stack') {
            steps {
                echo 'Launching MySQL and Backend via Docker Compose...'
                // --build forces Docker to pick up the new JAR file
                sh 'docker-compose up -d --build'
            }
        }

        stage('Verify Health') {
            steps {
                echo 'Waiting for services to be ready...'
                // Give the database and app a few seconds to initialize
                sleep 10
                sh 'docker ps'
            }
        }
    }

    post {
        success {
            echo 'SUCCESS: Deployment complete. Site live at https://onlyumniah.com'
            // Cleanup unused Docker images to save space on your 32GB laptop
            sh 'docker image prune -f'
        }
        failure {
            echo 'FAILURE: Build or Deployment failed. Check the logs below.'
        }
    }
}