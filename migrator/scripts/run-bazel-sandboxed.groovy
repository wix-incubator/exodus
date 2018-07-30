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
                         |--action_env=HOST_CONTAINER_NAME'''.stripMargin()
        HOST_CONTAINER_NAME = "bazel00"
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
                                             |$BAZEL test \\
                                             |      --flaky_test_attempts=3 \\
                                             |      ${env.BAZEL_FLAGS} \\
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
                    junit "bazel-testlogs/**/test.xml"
                    archiveArtifacts 'bazel-testlogs/**,bazel-out/**/test.outputs/outputs.zip'
                }
            }
        }
        regression{
            script{
                sendNotification(false)
            }
        }
        fixed{
            script{
                sendNotification(true)
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


def sendNotification(good) {
    def slack_file = "bazel_migration/slack_channels.txt"
    def channels = ['bazel-mig-alerts']
    if (fileExists(slack_file)) {
        channels = channels + (readFile(slack_file)).split(',')
    }
    if (good) {
        header = ":trophy: migration task '${env.JOB_NAME}' FIXED :trophy:"
        color = "good"
    } else {
        header = ":thumbsdown: migration task '${env.JOB_NAME}' REGRESSED :thumbsdown:"
        color = "warning"
    }
    def msg = compose(header)
    channels.each { channel ->
        slackSend channel: "#$channel", color: color, message: msg
    }
}


def compose(String header) {
    """*${header}*
    |===================================
    | *URL*: ${env.BUILD_URL}
    |${changesMessage()}
    |""".stripMargin()
}

def changesMessage() {
    def changeLogSets = currentBuild.changeSets
    def msg = []
    changeLogSets.each {
        def entries = it.items
        entries.each { entry ->
            msg += "${entry.commitId[0..5]}   ${entry.author.fullName}   [${new Date(entry.timestamp).format("MM-dd HH:mm")}]    ${entry.msg.take(30)}"
        }
    }
    def suffix = ""
    if (msg.isEmpty()){
        msg += "NO CHANGES"
    } else if (msg.size() > 5) {
        msg = msg.take(5)
        suffix = "\nsee more here ${env.BUILD_URL}/changes"
    }
    '*CHANGELOG:*\n```' + String.valueOf(msg.join("\n")) + '```' + suffix
}

