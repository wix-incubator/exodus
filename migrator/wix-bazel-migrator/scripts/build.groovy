pipeline {
    agent any
    options {
        timestamps()
    }
    tools {
        maven 'M3'
    }
    triggers{
        pollSCM('H/5 * * * *')
    }
    environment {
        MAVEN_INSTALL = "mvn clean install -B"
    }
    stages {
        stage('checkout') {
            steps {
                dir("bazel-migrator") {
                    git 'git@github.com:wix-private/bazel-migrator.git'
                }
                dir("wix-bazel-migrator") {
                    git branch: 'jenkins', url: 'git@github.com:wix-private/wix-bazel-migrator.git'
                }
            }
        }
        stage('build-migrator') {
            steps {
                dir("bazel-migrator") {
                    sh "${env.MAVEN_INSTALL}"
                }
                dir("wix-bazel-migrator") {
                    sh "${env.MAVEN_INSTALL}"
                }
            }
        }
    }
    post {
        always {
            junit "**/target/**/TEST-*.xml"
            archiveArtifacts "wix-bazel-migrator/target/wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
        }
    }
}