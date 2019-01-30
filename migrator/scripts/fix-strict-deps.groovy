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
        REPO_NAME = find_repo_name()
        bazel_log_file = "bazel-build.log"
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        BUILDOZER_HOME = tool name: 'buildozer', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        BUILDIFIER_HOME = tool name: 'buildifier', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$BUILDOZER_HOME/bin:$BUILDIFIER_HOME/bin:$JAVA_HOME/bin:$PATH"
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
        stage("bazel clean"){
            when{
                expression{CLEAN == "true"}
            }
            steps{
                dir("${env.REPO_NAME}") {
                    sh "bazel clean"
                }
            }
        }
        stage('build_and_fix') {
            steps {
                dir("${env.REPO_NAME}") {
                    script {
                        env.PUSH_TO_GIT = "false"
                        build_and_fix(env.ADDITIONAL_FLAGS_BAZEL_SIXTEEN_UP_LOCAL)
                    }
                }
            }
        }
        stage('push-to-git') {
            when {
                expression {
                    PUSH_TO_GIT == "true"
                }
            }
            steps {
                dir("${env.REPO_NAME}") {
                    sh """|git checkout ${env.BRANCH_NAME}
                          |git add "./*BUILD.bazel" .bazelrc
                          |git commit -m "strict deps fix by ${env.BUILD_URL}"
                          |git push origin ${env.BRANCH_NAME}
                          |""".stripMargin()
                }
            }
        }
    }
    post{
        always{
            script {
                if ("${env.TRIGGER_BUILD}" != "false") {
                    build job: "02-run-bazel", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
                    build job: "05-run-bazel-rbe", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
                    build job: "run-bazel-sandboxed", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
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

def build_and_fix(ADDITIONAL_FLAGS_BAZEL_SIXTEEN_UP_LOCAL) {
    status = sh(
            script: '''|#!/bin/bash
                       |# tee would output the stdout to file but will swallow the exit code
                       |bazel build ${ADDITIONAL_FLAGS_BAZEL_SIXTEEN_UP_LOCAL} -k --build_tag_filters=-deployable --strategy=Scalac=worker //... 2>&1 | tee bazel-build.log
                       |# retrieve the exit code
                       |exit ${PIPESTATUS[0]}
                       |'''.stripMargin(),
            returnStatus: true)
    build_log = readFile "bazel-build.log"
    if (build_log.contains("buildozer") || build_log.contains("[strict]")) {
        if (build_log.contains("Unknown label of file")){
            slackSend "Found 'Unknown label...' warning in ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|link>)"
        }
        echo "found strict deps issues"
        sh "python ../core-server-build-tools/scripts/fix_transitive.py"
        buildozerStatusCode = sh script: "buildozer -f bazel-buildozer-commands.txt", returnStatus: true
        if (buildozerStatusCode == 0) { // buildozer returns 3 when no action was needed
            env.PUSH_TO_GIT = "true"
            build_and_fix(ADDITIONAL_FLAGS_BAZEL_SIXTEEN_UP_LOCAL)
        } else {
            echo "buildozer exited with code ${buildozerStatusCode}"
            echo "[WARN] produced buildozer commands were not required!"
            currentBuild.result = 'UNSTABLE'
        }

    } else if (status == 0) {
        echo "No buildozer warnings were found"
        bazelrc = readFile(".bazelrc")
        if (bazelrc.contains("strict_java_deps=warn")) {
            writeFile file: ".bazelrc", text: bazelrc.replace("strict_java_deps=warn", "strict_java_deps=error")
            env.PUSH_TO_GIT = "true"
        }
    } else {
        echo "[WARN] No strict deps warnings found but build failed"
        currentBuild.result = 'UNSTABLE'
    }
}