pipeline {
    agent any
    options {
        timeout(time: 180, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
    }
    tools{
        jdk 'jdk8'
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
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
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
                echo "Running all tests excluding tests with tag 'docker'"
                script {
                    bazelrc = readFile(".bazelrc").replaceAll("build --disk_cache","# build --disk_cache")
                    writeFile file: ".bazelrc", text: bazelrc
                    wrap([
                        $class: 'LogfilesizecheckerWrapper',
                        'maxLogSize': 3000,
                        'failBuild': true,
                        'setOwn': true]) {

                            unstable_by_exit_code("UNIT", """|#!/bin/bash
                                                    |bazel ${env.BAZEL_STARTUP_OPTS} \\
                                                    |${env.BAZEL_COMMAND} \\
                                                    |      --test_tag_filters=-docker \\
                                                    |      --build_event_json_file=build.bep \\
                                                    |      ${env.BAZEL_FLAGS} \\
                                                    |      ${env.TEST_TARGET_LABEL}
                                                    |""".stripMargin())

                        }
                }
            }
        }
    }
    post {
        always {
            script {
                def invocation_id = findInvocations()
                link = "https://source.cloud.google.com/results/invocations/${invocation_id}/targets"
                println "See results here: ${link}"
                currentBuild.description = """<a href="$link" target="_blank">$invocation_id</a>"""

                if (env.BAZEL_COMMAND == "build" && env.FOUND_TEST == "true") {
                    archiveArtifacts 'bazel-testlogs/**,bazel-out/**/test.outputs/outputs.zip'
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
        case 1:
            echo "Build failed"
            env.FOUND_TEST = "true"
            currentBuild.result = 'FAILURE'
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
    bepLine = sh(script:"head -n 1 build.bep", returnStdout:true)
    bep = readJSON text: bepLine
    id = bep["started"]["uuid"]
    return id
}