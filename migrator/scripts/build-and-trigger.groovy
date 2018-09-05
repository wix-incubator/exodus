pipeline {
    agent any
    options {
        timestamps()
    }
    tools {
        maven 'M3'
    }
    environment {
        MAVEN_INSTALL = "mvn clean install -U -T 1C -B -pl migrator/wix-bazel-migrator -am"
    }
    stages {
        stage('checkout') {
            steps {
                git url: 'git@github.com:wix-private/bazel-tooling.git', branch: "${SCM_BRANCH}", credentialsId: 'builduser-git'
            }
        }
        stage('build-migrator') {
            steps {
                sh "${env.MAVEN_INSTALL}"
            }
        }
    }
    post {
        success {
            archiveArtifacts "migrator/wix-bazel-migrator/target/wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
            script {
                job = "${repo_name}/${env.debug_job_name}"
                build job: job, parameters: [string(name: 'MIGRATOR_BUILD_JOB', value: '/Migrator-dry-run-Specific'), booleanParam(name: 'skip_classpath', value: false)], wait: false, propagate: false
            }
        }
    }
}