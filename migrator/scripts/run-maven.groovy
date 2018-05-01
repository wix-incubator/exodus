pipeline {
    agent any
    options {
        timestamps()
        timeout(time: 180, unit: 'MINUTES')
    }
    tools {
        maven 'M3'
    }
    environment {
        MAVEN_OPTS = "-Xmx8G -XX:MaxMetaspaceSize=1G -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
        MAVEN_INSTALL = "mvn clean install -B -Dwix.environment=CI -DtestFailureIgnore=true -DshouldSkipAssembly=true"
        JAVA_HOME = tool name: 'jdk8u152'
        PATH = "$JAVA_HOME/bin:$PATH"
        REPO_NAME = find_repo_name()
        COMMIT_HASH = "${env.COMMIT_HASH}"
    }
    stages {
        stage('checkout') {
            steps {
                 dir("${env.REPO_NAME}") {
                     echo "got commit hash: ${env.COMMIT_HASH}"
                     checkout([$class: 'GitSCM', branches: [[name: env.COMMIT_HASH ]],
                               userRemoteConfigs: [[url: "${env.repo_url}"]]])
                }
            }
        }
        stage('mvn') {
            steps {
                script {
                    dir("${env.REPO_NAME}") {
                        if (fileExists('./pom.xml')) {
                            sh "${env.MAVEN_INSTALL}"
                        } else {
                            def root = pwd()
                            def dirs =  sh(returnStdout: true, script: "ls -d ${root}/*").trim().split(System.getProperty("line.separator"))
                            dirs.each {
                                pom = it + "/pom.xml"
                                if (fileExists(pom)) 
                                    dir(it) {
                                        sh "${env.MAVEN_INSTALL}"
                                    }
                            }
                        }
                    } 
                }
            }
        }
    }
    post {
        always {
            script {
                if (findFiles(glob: '**/target/**/TEST-*.xml').any()) {
                    archiveArtifacts "**/target/**/TEST-*.xml"
                }
                if (findFiles(glob: '**/bazel_migration/*.*').any()) {
                    archiveArtifacts "**/bazel_migration/*.*"
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



@SuppressWarnings("GroovyAssignabilityCheck")
def find_repo_name() {
    name = "${env.repo_url}".split('/')[-1]
    if (name.endsWith(".git"))
        name = name[0..-5]
    return name
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
