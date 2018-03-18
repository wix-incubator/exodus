// must have sandbox off so you must paste this code directly into pipeline job (don't choose SCM)
import jenkins.*

node {
    def migrate_repo = [:]
    def folders = all_folders()
    def count = 0
    folders.each {
        def name = it.job_name
        def repo = it.repo
        def migrate = name + "/01-migrate"
        def fix_deps = name + "/03-fix-strict-deps"
        def compare = name + "/03-compare"
        def run_bazel = name + "/02-run-bazel"
        def run_maven = name + "/02-run-maven"
        def run_rbe = name + "/05-run-bazel-rbe"
        def delay = "${env.delay_between_trigger}".toInteger()
        def sleep_time = count * delay
        def seq = {
            sh "sleep $sleep_time"
            echo "STARTED E2E MIGRATION"
            def git_commit_hash = sh(script: "git ls-remote -q ${repo} | head -1 | cut -f 1", returnStdout: true)
            parallel(
                    "bazel-$name": {
                        echo "[$name] Git commit hash: ${git_commit_hash}"
                        def migrate_run = build job: migrate, wait: true, propagate: false,
                                parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false), string(name: 'COMMIT_HASH', value: git_commit_hash)]
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
                    "maven-$name": {
                        build job: run_maven, wait: true, propagate: false,
                                parameters: [string(name: 'COMMIT_HASH', value: git_commit_hash), booleanParam(name: 'CLEAN', value: false)]
                    }
            )
            build job: compare, wait: false
            echo "FINISHED"
            return true
        }
        count += 1
        migrate_repo[name] = seq
    }
    parallel migrate_repo
}

@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {
        it.description.startsWith("Migration")
    }.collect {
        [job_name: it.name, repo: it.description.substring("Migration to ".length())]
    }
}