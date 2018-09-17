pipeline {
    agent any
    options {
        timeout(time: 15, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }
    tools{
        jdk 'jdk8'
    }
    environment {
        GOOGLE_APPLICATION_CREDENTIALS = credentials("rbe_credentials")
        BAZEL_STARTUP_OPTS = '''|--bazelrc=.bazelrc.remote \\
                                |'''.stripMargin()
        BAZEL_FLAGS = '''|-k \\
                         |--config=remote \\
                         |--config=results \\
                         |--project_id=gcb-with-custom-workers \\
                         |--remote_instance_name=projects/gcb-with-custom-workers/instances/default_instance'''.stripMargin()
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
    }
    stages {
        stage('checkout-branch') {
            steps {
                git branch: "${env.SCM_BRANCH}", url : "git@github.com:wix-private/bazel-tooling.git"
            }
        }
        stage('build-migrator') {
            steps {
                script{
                    sh  """|#!/bin/bash
                           |bazel ${env.BAZEL_STARTUP_OPTS} \\
                           |build \\
                           |      ${env.BAZEL_FLAGS} \\
                           |      //migrator/wix-bazel-migrator:migrator_cli_deploy.jar
                           |""".stripMargin()

                    sh "mkdir -p migrator/wix-bazel-migrator/target/"
                    sh "mv bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar migrator/wix-bazel-migrator/target/wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
                }
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