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
        stage('run-e2e') {
            echo "sleeping ${sleep_time}"
            def git_commit_hash = sh(script: "git ls-remote -q ${repo} | head -1 | cut -f 1", returnStdout: true)
            parallel(
                    "bazel": {
                        echo "Git commit hash: ${git_commit_hash}"
                        def migrate_run = build job: migrate, wait: true, propagate: false,
                                parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false), string(name: 'COMMIT_HASH', value: git_commit_hash)], quietPeriod: sleep_time
                        if (migrate_run.result == "SUCCESS") {
                            def migration_branch = "bazel-mig-${migrate_run.number}"
                            def parameters = [
                                    string(name: 'BRANCH_NAME', value: migration_branch),
                                    booleanParam(name: 'CLEAN', value: false),
                                    string(name: 'COMMIT_HASH', value: git_commit_hash)
                            ]
                            build job: fix_deps, wait: true, propagate: false, parameters: parameters + booleanParam(name: 'TRIGGER_BUILD', value: false)
                            build job: run_rbe, wait: false, propagate: false, parameters: parameters
                            build job: run_bazel, wait: true, propagate: false, parameters: parameters
                        }
                    },
                    "maven": {
                        build job: run_maven, wait: true, propagate: false,
                                parameters: [string(name: 'COMMIT_HASH', value: git_commit_hash), booleanParam(name: 'CLEAN', value: false)], quietPeriod: sleep_time
                    }
            )
            build job: compare, wait: false
        }
    }
}