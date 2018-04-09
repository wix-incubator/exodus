pipeline {
    agent any
    options {
        timeout(time: 180, unit: 'MINUTES')
        timestamps()
    }
    environment {
        BAZEL_FLAGS = '''|-k \\
                         |--strategy=Scalac=worker \\
                         |--experimental_sandbox_base=/dev/shm \\
                         |--sandbox_tmpfs_path=/tmp \\
                         |--test_output=errors \\
                         |--test_arg=--jvm_flags=-Dcom.google.testing.junit.runner.shouldInstallTestSecurityManager=false \\
                         |--test_arg=--jvm_flags=-Dwix.environment=CI'''.stripMargin()
        DOCKER_HOST = "${env.TEST_DOCKER_HOST}"
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        JAVA_HOME = tool name: 'jdk8u152'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
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
        stage('build') {
            steps {
                sh "bazel info"
                sh "bazel build --strategy=Scalac=worker //..."
            }
        }
        stage('UT') {
            steps {
                script {
                    unstable_by_exit_code("UNIT", """|#!/bin/bash
                                             |bazel test \\
                                             |      --test_tag_filters=UT,-IT \\
                                             |      --flaky_test_attempts=3 \\
                                             |      ${env.BAZEL_FLAGS} \\
                                             |      //...
                                             |""".stripMargin())
                }
            }
        }
        stage('IT') {
            steps {
                script {
                    unstable_by_exit_code("IT/E2E", """|#!/bin/bash
                                             |export DOCKER_HOST=$env.TEST_DOCKER_HOST
                                             |bazel test \\
                                             |      --test_tag_filters=IT \\
                                             |      --strategy=TestRunner=standalone \\
                                             |      ${env.BAZEL_FLAGS} \\
                                             |      --test_env=DOCKER_HOST \\
                                             |      --jobs=1 \\
                                             |      //...
                                             |""".stripMargin())
                }
            }
        }
    }
    post {
        always {
            script {
                if (env.FOUND_TEST == "true") {
                    archiveArtifacts 'bazel-out/**/testlogs/**/*.log,bazel-testlogs/**/test.xml,bazel-out/**/test.outputs/outputs.zip'
                    junit "bazel-testlogs/**/test.xml"
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
