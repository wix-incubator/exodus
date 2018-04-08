pipeline {
    agent any
    options {
        timestamps()
        timeout(time: 180, unit: 'MINUTES')
    }
    tools {
        maven 'M3'
    }
    environment {
        MAVEN_INSTALL = "export MAVEN_OPTS='-Xmx8G';mvn clean install -B -Dwix.environment=CI -DtestFailureIgnore=true -DshouldSkipAssembly=true"
        JAVA_HOME = tool name: 'jdk8u152'
        REPO_NAME = find_repo_name()
        COMMIT_HASH = "${env.COMMIT_HASH}"
    }
    stages {
        stage('checkout') {
            steps {
                 dir("${env.REPO_NAME}") {
                     echo "got commit hash: ${env.COMMIT_HASH}"
                     checkout([$class: 'GitSCM', branches: [[name: env.COMMIT_HASH ]],
                               userRemoteConfigs: [[url: "${env.repo_url}"]]])
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
            script {
                if (findFiles(glob: '**/target/**/TEST-*.xml').any()) {
                    archiveArtifacts "**/target/**/TEST-*.xml"
                }
                if (findFiles(glob: '**/bazel_migration/*.*').any()) {
                    archiveArtifacts "**/bazel_migration/*.*"
                }
            }
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
