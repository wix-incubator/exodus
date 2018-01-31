pipeline {
    agent any
    options {
        timestamps()
        throttle(categories: ['migrate'])
    }
    environment {
        CODOTA_TOKEN = credentials("codota-token")
        REPO_NAME = find_repo_name()
        BRANCH_NAME = "bazel-mig-${env.BUILD_ID}"
        bazel_log_file = "bazel-build.log"
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        JAVA_HOME = tool name: 'jdk8u152'
        PATH = "$BAZEL_HOME/bin:$JAVA_HOME/bin:$PATH"
    }
    stages {
        stage('checkout') {
            steps {
                dir("wix-bazel-migrator") {
                    copyArtifacts flatten: true, projectName: '../Migrator-build', selector: lastSuccessful()
                }
                dir("${env.REPO_NAME}") {
                    git "${env.repo_url}"
                }
            }
        }
        stage('migrate') {
            steps {
                dir("${env.REPO_NAME}") {
                    sh 'rm -rf third_party'
                    sh 'find . -path "*/*BUILD" -exec rm -f {} \\;'
                }
                dir("wix-bazel-migrator") {
                    sh "java -Xmx12G -Dcodota.token=${env.CODOTA_TOKEN} -Dclean.codota.analysis.cache=true -Dskip.classpath=false -Dskip.transformation=false -Dfail.on.severe.conflicts=true -Drepo.root=../${repo_name} -jar wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
                }
            }
        }
        stage('post-migrate') {
            steps {
                dir("${env.REPO_NAME}") {
                    sh "buildozer 'add tags manual' //third_party/...:%scala_import"
                    sh 'buildifier $(find . -iname BUILD -type f)'
                    sh 'touch .gitignore'
                    sh 'grep -q -F "/bazel-*" .gitignore || echo "\n/bazel-*" >> .gitignore'
                    script{
                        if (fileExists('bazel_migration/post-migration.sh')){
                            sh "bazel_migration/post-migration.sh"
                        }
                    }
                }
            }
        }
        stage('fix-strict-deps'){
            steps{
                dir("core-server-build-tools") {
                    git 'git@github.com:wix-private/core-server-build-tools.git'
                }
                dir("${env.REPO_NAME}") {
                    script {
                        build_and_fix()
                    }
                }
            }
        }
        stage('push-to-git') {
            steps {
                dir("${env.REPO_NAME}"){
                   sh """|git checkout -b ${env.BRANCH_NAME}
                         |git add .
                         |git reset -- bazel-build.log
                         |git commit -m "bazel migrator created by ${env.BUILD_URL}"
                         |git push origin ${env.BRANCH_NAME}
                         |""".stripMargin()
                    // WARNING: carefully test any change you make to the following line. mistakes here can be fatal!!
                    sh "git branch -r | grep bazel-mig | sort -n -r -t \"-\" -k 3 | tail -n +6 | sed  -e 's/origin\\//git push origin :/g' | sh"
                }
            }
        }
    }
    post {
        always {
            script {
                try {
                    dir("wix-bazel-migrator") {
                        echo "[INFO] creating tar.gz files for migration artifacts..."
                        sh """|tar czf classpathModules.cache.tar.gz classpathModules.cache
                              |tar czf cache.tar.gz cache
                              |tar czf dag.bazel.tar.gz dag.bazel""".stripMargin()
                    }
                } catch (err) {
                    echo "[WARN] could not create all tar.gz files ${err}"
                } finally {
                    archiveArtifacts "wix-bazel-migrator/classpathModules.cache.tar.gz,wix-bazel-migrator/dag.bazel.tar.gz,wix-bazel-migrator/cache.tar.gz"
                }
            }
        }
        success{
            script{
                if ("${env.TRIGGER_BUILD}" != "false"){
                    build job: "02-run-bazel", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
                    build job: "05-run-bazel-rbe", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
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

def build_and_fix() {
    status = sh(
            script: '''|#!/bin/bash
                       |# tee would output the stdout to file but will swallow the exit code
                       |bazel build -k --strategy=Scalac=worker //... 2>&1 | tee bazel-build.log
                       |# retrieve the exit code
                       |exit ${PIPESTATUS[0]}
                       |'''.stripMargin(),
            returnStatus: true)
    build_log = readFile "bazel-build.log"
    if (build_log.contains("buildozer")) {
        if (build_log.contains("Unknown label of file")){
            slackSend "Found 'Unknown label...' warning in ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|link>)"
        }
        echo "found strict deps issues"
        sh "python ../core-server-build-tools/scripts/fix_transitive.py"
        sh "buildozer -f bazel-buildozer-commands.txt"
        build_and_fix()
    } else if (status == 0) {
        echo "No buildozer warnings were found"
        bazelrc = readFile(".bazelrc")
        if (bazelrc.contains("strict_java_deps=warn")) {
            writeFile file: ".bazelrc", text: bazelrc.replace("strict_java_deps=warn", "strict_java_deps=error")
        }
    } else {
        echo "[WARN] No strict deps warnings found but build failed"
    }
}
