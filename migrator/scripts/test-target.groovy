pipeline {
    agent any
    options {
        timeout(time: 80, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }
    tools{
        jdk 'jdk8'
    }
    environment {
        BAZEL_FLAGS = '''|-k \\
                         |--experimental_sandbox_base=/dev/shm \\
                         |--test_arg=--jvm_flags=-Dwix.environment=CI \\
                         |--action_env=HOST_NETWORK_NAME'''.stripMargin()
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
        BAZEL = "bazel --host_javabase=$JAVA_HOME"
    }
    stages {
        stage('checkout') {
            steps {
                git branch: "${env.BRANCH_NAME}", url: "${env.repo_url}"
            }
        }
        stage('pre-build') {
            steps {
                sh "touch tools/ci.environment"
            }
        }
        stage('test') {
            steps {
                script {
                    unstable_by_exit_code("UNIT", """|#!/bin/bash
                                             |$BAZEL ${env.BAZEL_COMMAND} \\
                                             |      --flaky_test_attempts=3 \\
                                             |      ${env.BAZEL_FLAGS} \\
                                             |      ${env.TEST_TARGET_LABEL}
                                             |""".stripMargin())
                }
            }
        }
    }
    post {
        always {
            script {
                if (env.BAZEL_COMMAND == "test" && env.FOUND_TEST == "true") {
                    junit "bazel-testlogs/**/test.xml"
                    archiveArtifacts 'bazel-testlogs/**,bazel-out/**/test.outputs/outputs.zip'
                }
            }
        }
    }
}

@SuppressWarnings("GroovyUnusedDeclaration")
def unstable_by_exit_code(phase, some_script) {
    echo "Running " + some_script
    return_code = a = sh(script: some_script, returnStatus: true)
    switch (a) {
        case 0:
            env.FOUND_TEST = "true"
            break
        case 1:
            echo "Build failed"
            env.FOUND_TEST = "true"
            currentBuild.result = 'FAILURE'
            break
        case 3:
            echo "There were test failures"
            env.FOUND_TEST = "true"
            currentBuild.result = 'UNSTABLE'
            break
        case 4:
            echo "***NO ${phase} TESTS WERE FOUND! IF YOU HAVE SUCH TESTS PLEASE DEBUG THIS WITH THE BAZEL PEOPLE***"
            break
        default:
            currentBuild.result = 'FAILURE'
    }
}