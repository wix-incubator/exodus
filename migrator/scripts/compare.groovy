pipeline {
    agent any
    stages {
        stage('should-fail') {
            when { not { environment name: "MAVEN_SUCCESS", value: "true" } }
            steps {
                script {
                    error("Unable to perform comparison, maven run did not finish successfully.")
                }
            }
        }
        stage('checkout') {
            steps {
                dir("${env.REPO_NAME}") {
                    git url: "${env.repo_url}", branch: "${BRANCH_NAME}"
                }
            }
        }
        stage('maven artifacts') {
            steps {
                script {
                    try {
                        copyArtifacts projectName: '02-run-maven', target: 'maven-output', selector: specific("${MAVEN_RUN_NUMBER}")
                    } catch (err) {
                        echo "[WARN] unable to copy maven artifacts, perhaps none exist?"
                    }
                }
            }
        }
        stage('bazel artifacts') {
            steps {
                script {
                    try {
                        copyArtifacts projectName: '02-run-bazel', filter: '**/*.xml', target: 'bazel-output', selector: specific("${BAZEL_RUN_NUMBER}")
                    } catch (err) {
                        echo "[WARN] unable to copy bazel artifacts, perhaps none exist?"
                    }
                }
            }
        }
        stage('compare') {
            steps {
                script {
                    if (!has_artifacts('bazel') && !has_artifacts('maven')) {
                        echo "No tests were detected in both maven and bazel"
                        currentBuild.result = 'UNSTABLE'
                        env.ALLOW_MERGE = "false"
                        return
                    }

                    dir('core-server-build-tools') {
                        git "git@github.com:wix-private/core-server-build-tools.git"
                        ansiColor('xterm') {
                            sh """|cd scripts
                                  |pip3 install --user --extra-index-url https://pypi.python.org/simple -r requirements.txt
                                  |python3 -u maven_bazel_diff.py maven-count ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                                  |""".stripMargin()
                        }
                    }

                    if (!has_artifacts('bazel')) {
                        echo "[WARN] Unable to perform comparison - bazel artifacts were not found."
                        currentBuild.result = 'UNSTABLE'
                        env.ALLOW_MERGE = "false"
                        return
                    }

                    dir('core-server-build-tools') {
                        git "git@github.com:wix-private/core-server-build-tools.git"
                        ansiColor('xterm') {
                            sh """|export PYTHONIOENCODING=UTF-8
                                  |cd scripts
                                  |pip3 install --user --extra-index-url https://pypi.python.org/simple -r requirements.txt
                                  |python3 -u maven_bazel_diff.py maven-count ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                                  |python3 -u maven_bazel_diff.py compare ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                                  |""".stripMargin()
                        }
                    }
                    env.ALLOW_MERGE_FILE_EXISTS = "${fileExists "${env.REPO_NAME}/bazel_migration/auto-merge"}"
                }
            }
        }
        stage("auto-merge"){
            when{
                environment name : "ALLOW_MERGE", value : "true"
                environment name : "ALLOW_MERGE_FILE_EXISTS", value : "true"
            }
            steps{
                 dir("${env.REPO_NAME}"){
                      sh """|git checkout ${env.BRANCH_NAME}
                            |git commit --allow-empty -m "passed compare - #automerge"
                            |git push origin ${env.BRANCH_NAME}
                            |""".stripMargin()
                 }    
            }
        }
    }
    post {
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

def has_artifacts(dir) {
    return findFiles(glob: "${dir}-output/**/*.xml").any()
}


def sendNotification(good) {
    def slack_file = "${env.REPO_NAME}/bazel_migration/slack_channels.txt"
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
