pipeline {
    agent any
    options {
        timestamps()
    }
    environment {
        LATEST_COMMIT_HASH_COMMAND = "git ls-remote -q ${env.repo_url} | head -1 | cut -f 1"
        // if migration failed - tell downstream jobs to read the master
        MIGRATION_BRANCH = "master"
    }
    stages {
        stage('setup') {
            steps {
                script {
                    env.GIT_COMMIT_HASH = sh(script: "${env.LATEST_COMMIT_HASH_COMMAND}", returnStdout: true)
                }
            }
        }
        stage('bazel-and-maven') {
            parallel {
                stage('bazel') {
                    steps {
                        script {
                            println "Commit hash: ${env.GIT_COMMIT_HASH}"
                            def migrate_run = build job: "01-migrate", wait: true, propagate: false, parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false), string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")]
                            if (migrate_run.result == "SUCCESS") {
                                def migration_branch = "bazel-mig-${migrate_run.number}"
                                def parameters = [
                                        string(name: 'BRANCH_NAME', value: migration_branch),
                                        booleanParam(name: 'TRIGGER_BUILD', value: false),
                                        booleanParam(name: 'CLEAN', value: false),
                                        string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")
                                ]
                                build job: "03-fix-strict-deps", wait: true, propagate: false, parameters: parameters
                                build job: "05-run-bazel-rbe", wait: false, propagate: false, parameters: parameters
                                bazel_run = build job: "02-run-bazel", wait: true, propagate: false, parameters: parameters
                                env.BAZEL_SUCCESS = "${bazel_run.result == "SUCCESS"}"
                                env.BAZEL_RUN_NUMBER = "${bazel_run.number}"
                                env.MIGRATION_BRANCH = migration_branch
                            }
                        }
                    }
                }
                stage('maven') {
                    steps {
                        script {
                            def m = build job: "02-run-maven", wait: true, propagate: false, parameters: [string(name: 'COMMIT_HASH', value: "${env.GIT_COMMIT_HASH}")]
                            env.MAVEN_RUN_NUMBER = "${m.number}"
                            env.MAVEN_SUCCESS = "${m.result == "SUCCESS"}"
                        }
                    }
                }
            }
        }
        stage('compare') {
            steps {
                script {
                    params = [
                        string(name: "ALLOW_MERGE", value: env.BAZEL_SUCCESS),
                        string(name: 'MAVEN_SUCCESS', value: env.MAVEN_SUCCESS),
                        string(name: 'BAZEL_RUN_NUMBER', value: env.BAZEL_RUN_NUMBER),
                        string(name: 'MAVEN_RUN_NUMBER', value : env.MAVEN_RUN_NUMBER),
                        string(name: 'BRANCH_NAME', value : env.MIGRATION_BRANCH)
                    ]
                    build job: "03-compare", wait: true, parameters: params
                }
            }
        }
    }
}