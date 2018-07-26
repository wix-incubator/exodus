pipeline {
    agent any
    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
    }
    environment {
        BAZEL_FLAGS = '''|--strategy=Scalac=worker \\
                         |--experimental_sandbox_base=/dev/shm \\
                         |--sandbox_tmpfs_path=/tmp \\
                         |--test_output=errors \\
                         |--test_arg=--jvm_flags=-Dcom.google.testing.junit.runner.shouldInstallTestSecurityManager=false \\
                         |--test_env=LC_ALL="en_US.UTF-8" \\
                         |--test_arg=--jvm_flags=-Dwix.environment=CI'''.stripMargin()
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
        stage('Test') {
            steps {
                script {
                    if (env.IT == "false") {
                        unstable_by_exit_code("""|#!/bin/bash
                                             |bazel test \\
                                             |      ${env.BAZEL_FLAGS} \\
                                             |      ${TEST_TARGET_LABEL}
                                             |""".stripMargin())
                    } else {
                        unstable_by_exit_code("""|#!/bin/bash
                                             |bazel test \\
                                             |      --strategy=TestRunner=standalone \\
                                             |      ${env.BAZEL_FLAGS} \\
                                             |      --test_env=HOST_CONTAINER_NANE=bazel00 \\
                                             |      --jobs=1 \\
                                             |      ${TEST_TARGET_LABEL}
                                             |""".stripMargin())
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                if (env.FOUND_TEST == "true") {
                    archiveArtifacts 'bazel-out/**/test.log'
                    junit "bazel-testlogs/**/test.xml"
                }
            }
        }
    }
}

@SuppressWarnings("GroovyUnusedDeclaration")
def unstable_by_exit_code(some_script) {
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
        echo "***NO TESTS WERE FOUND! IF YOU HAVE SUCH TESTS PLEASE DEBUG THIS WITH THE BAZEL PEOPLE***"
            break
        default:
            currentBuild.result = 'FAILURE'
    }
}
