def migration_branch = null
def bazel_success = false
def maven_success = false

pipeline {
    agent any
    options {
        timestamps()
    }
    environment {
        LATEST_COMMIT_HASH_COMMAND = "git ls-remote -q ${env.repo_url} | head -1 | cut -f 1"
    }
    stages {
        stage('setup') {
            steps {
                script {
                    env.GIT_COMMIT_HASH = sh(script: "${env.LATEST_COMMIT_HASH_COMMAND}", returnStdout: true)
                }
            }
        }
        stage('run-migration-e2e') {
            steps {
                parallel(
                        "migrate": {
                            script {
                                println "Commit hash: ${env.GIT_COMMIT_HASH}"
                                def migrate_run = build job: "01-migrate", wait: true, propagate: true, parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false), string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")]
                                migration_branch = "bazel-mig-${migrate_run.number}"
                                BUILD_PARAMETERS = [
                                        string(name: 'BRANCH_NAME', value: migration_branch),
                                        booleanParam(name: 'CLEAN', value: false),
                                        string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")
                                ]
                            }
                        },
                        "maven": {
                            script {
                                def m = build job: "02-run-maven", wait: true, propagate: false, parameters: [string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")]
                                maven_success = (m.result == "SUCCESS") || (m.result == "UNSTABLE")
                            }
                        }
                )
            }
        }
        stage('build-and-test') {
            steps {
                script {
                    build job: "03-fix-strict-deps", wait: true, propagate: false, parameters: env.BUILD_PARAMETERS
                    build job: "05-run-bazel-rbe", wait: false, propagate: false, parameters: env.BUILD_PARAMETERS
                    def b = build job: "02-run-bazel", wait: true, propagate: false, parameters: env.BUILD_PARAMETERS
                    bazel_success = (b.result == "SUCCESS") || (b.result == "UNSTABLE")
                }
            }
        }
        stage('compare') {
            steps {
                script {
                    if (bazel_success && maven_success) {
                        build job: "03-compare", wait: true
                    } else {
                        error("bazel run or maven run failed")
                    }
                }
            }
        }
    }
}