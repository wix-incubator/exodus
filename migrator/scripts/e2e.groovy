def migration_branch = null
def bazel_success = false
def maven_success = false

pipeline {
    agent any
    options {
        timestamps()
    }
    stages {
        stage('migrate') {
            steps {
                script {
                    def migrate_run = build job: "01-migrate", wait: true, propagate: true, parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false)]
                    migration_branch = "bazel-mig-${migrate_run.number}"
                }
            }
        }
        stage('build-and-test') {
            steps {
                parallel(
                        "bazel": {
                            script {
                                build job: "03-fix-strict-deps", wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false),booleanParam(name: 'TRIGGER_BUILD', value: false)]
                                build job: "05-run-bazel-rbe", wait: false, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                                def b = build job: "02-run-bazel", wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                                bazel_success = (b.result == "SUCCESS") || (b.result == "UNSTABLE")
                            }
                        },
                        "maven": {
                            script {
                                def m = build job: "02-run-maven", wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                                maven_success = (m.result == "SUCCESS") || (m.result == "UNSTABLE")
                            }
                        }
                )
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