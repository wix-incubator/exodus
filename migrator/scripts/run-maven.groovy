import groovy.io.FileType

pipeline {
    agent any
    options {
        timestamps()
    }
    tools {
        maven 'M3'
    }
    environment {
        MAVEN_INSTALL = "mvn clean install -B -Dwix.environment=CI -DtestFailureIgnore=true"
        JAVA_HOME = tool name: 'jdk8u152'
        REPO_NAME = find_repo_name()
    }
    stages {
        stage('checkout') {
            steps {
                 dir("${env.REPO_NAME}") {
                    git url: "${env.repo_url}", branch: "${env.BRANCH_NAME}"
                }
            }
        }
        stage('mvn') {
            steps {
                script {
                    dir("${env.REPO_NAME}") {
                        if (fileExists('./pom.xml')) {
                            sh "${env.MAVEN_INSTALL}"
                        } else {
                            def root = pwd()
                            def dirs =  sh(returnStdout: true, script: "ls -d ${root}/*").trim().split(System.getProperty("line.separator"))
                            dirs.each {
                                pom = it + "/pom.xml"
                                if (fileExists(pom)) 
                                    dir(it) {
                                        sh "${env.MAVEN_INSTALL}"
                                    }
                            }
                        }
                    } 
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