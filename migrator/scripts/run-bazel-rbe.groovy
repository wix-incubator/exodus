pipeline {
    agent any
    options {
        timeout(time: 180, unit: 'MINUTES')
        timestamps()
    }
    environment {
        GOOGLE_APPLICATION_CREDENTIALS = credentials("rbe_credentials")
        BAZEL_STARTUP_OPTS = '''|--bazelrc=.bazelrc.remote \\
                                |'''.stripMargin()
        BAZEL_FLAGS = '''|-k \\
                         |--config=remote \\
                         |--config=results \\
                         |--project_id=gcb-with-custom-workers \\
                         |--remote_instance_name=projects/gcb-with-custom-workers \\
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
        stage('Test') {
            steps {
                sh "bazel info"
                echo "Running all tests excluding tests with tag 'docker'"
                script {
                    wrap([
                        $class: 'LogfilesizecheckerWrapper',
                        'maxLogSize': 3000,
                        'failBuild': true,
                        'setOwn': true]) {
                            tee('bazel-log.output') {
                            unstable_by_exit_code("UNIT", """|#!/bin/bash
                                                    |bazel ${env.BAZEL_STARTUP_OPTS} \\
                                                    |test \\
                                                    |      --test_tag_filters=-docker \\
                                                    |      ${env.BAZEL_FLAGS} \\
                                                    |      //...
                                                    |""".stripMargin())
                            }
                        }
                }
            }
        }
    }
    post {
        always {
            script {
                println(findInvocations())
                findInvocations().each{invocation_id->
                    link = "https://source.cloud.google.com/results/invocations/${invocation_id}/targets"
                    println "See results here: ${link}"
                    currentBuild.description = """<a href="$link" target="_blank">$invocation_id</a>"""
                }
                if (env.FOUND_TEST == "true") {
                    junit allowEmptyResults: true, testResults: "bazel-testlogs/**/test.xml"
                    archiveArtifacts 'bazel-out/**/testlogs/**/*.log,bazel-testlogs/**/test.xml,bazel-out/**/test.outputs/outputs.zip'
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

def findInvocations(){
    log = readFile('bazel-log.output')
    def match = log =~ /.+invocation_id: (.+)/
    if (match.size() > 0) {
        def id = match[0][1]
        println ("id = $id")
        return [id]
    } else {
        println ("[WARN] could not find remote invocation id")
        return []
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
