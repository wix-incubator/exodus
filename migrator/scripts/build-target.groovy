pipeline {
    agent any
    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
    }
    tools {
        jdk 'jdk8'
    }
    environment {
        BAZEL_FLAGS = '''|--strategy=Scalac=worker \\
                         |--experimental_sandbox_base=/dev/shm \\
                         |--sandbox_tmpfs_path=/tmp'''.stripMargin()
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
    }
    stages {
        stage('checkout') {
            steps {
                deleteDir()
                git branch: "${env.BRANCH_NAME}", url: "${env.repo_url}"
            }
        }
        stage('build') {
            steps {
                sh "bazel build ${TARGET_LABEL}"
            }
        }
    }
}