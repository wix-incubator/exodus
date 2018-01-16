pipeline {
    agent any
    options {
        timestamps()
    }
    tools {
        maven 'M3'
    }
    environment {
        MAVEN_INSTALL = "mvn clean install -B -Dwix.environment=CI"
        REPO_NAME = find_repo_name()
    }
    stages {
        stage('checkout') {
            steps {
                dir("${env.REPO_NAME}") {
                    git "${env.repo_url}"
                }
            }
        }
        stage('mvn') {
            steps {
                dir("${env.REPO_NAME}") {
                    sh "${env.MAVEN_INSTALL}"
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts "**/target/**/TEST-*.xml"
        }
    }
}

@SuppressWarnings("GroovyAssignabilityCheck")
def find_repo_name() {
    name = "${env.repo_url}".split('/')[-1]
    if (name.endsWith(".git"))
        name = name[0..-5]
    return name
}