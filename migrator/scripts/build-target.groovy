pipeline {
    agent any
    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
    }
    environment {
        BAZEL_FLAGS = '''|--strategy=Scalac=worker \\
                         |--experimental_sandbox_base=/dev/shm \\
                         |--sandbox_tmpfs_path=/tmp'''.stripMargin()
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
        stage('build') {
            steps {
                sh "bazel build ${TEST_TARGET_LABEL}"
            }
        }
    }
}