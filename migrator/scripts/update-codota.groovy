pipeline {
    agent any
    options {
        timestamps()
        ansiColor('xterm')
    }
    tools{
        jdk 'jdk8'
    }
    environment {
        CODOTA_TOKEN = credentials("codota-token")
        REPO_NAME = find_repo_name()
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
    }
    stages {
        stage('checkout') {
            steps {
                dir("core-server-build-tools") {
                    git 'git@github.com:wix-private/core-server-build-tools.git'
                }
                dir("${env.REPO_NAME}") {
                    git branch: "${env.BRANCH_NAME}", url: "${env.repo_url}"
                    sh "git clean -fd"
                }
            }

        }
        stage('update_codota') {
            steps {
                dir("${env.REPO_NAME}") {
                    ansiColor('xterm') {
                        sh "python -u ../core-server-build-tools/scripts/inter-repo/codota-update.py"
                    }
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