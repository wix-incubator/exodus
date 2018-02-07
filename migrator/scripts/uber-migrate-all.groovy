// must have sandbox off so you must paste this code directly into pipeline job (don't choose SCM)
import jenkins.*

node {
    def migrate_repo = [:]
    def folders = all_folders()
    def count = 0
    folders.each {
        def migrate = it + "/01-migrate"
        def fix_deps = it + "/03-fix-strict-deps"
        def compare = it + "/03-compare"
        def run_bazel = it + "/02-run-bazel"
        def run_maven = it + "/02-run-maven"
        def run_rbe = it + "/05-run-bazel-rbe"
        def delay = "${env.delay_between_trigger}" as Integer
        def sleep_time = count * delay
        def seq = {
            echo "sleeping ${sleep_time}"
            def migrate_run = build job: migrate, wait: true, propagate: false, parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false)], quietPeriod: sleep_time
            def migration_branch = "bazel-mig-${migrate_run.number}"

            if (migrate_run.result == "SUCCESS") {
                def bazel_success = false
                def maven_success = false
                parallel(
                        "bazel": {
                            build job: run_rbe, wait: false, propagate: false,  parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                            build job: fix_deps, wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false),booleanParam(name: 'TRIGGER_BUILD', value: false)]
                            def b = build job: run_bazel, wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                            bazel_success = (b.result == "SUCCESS")

                        },
                        "maven": {
                            def m = build job: run_maven, wait: true, propagate: false, parameters: [string(name: 'BRANCH_NAME', value: migration_branch), booleanParam(name: 'CLEAN', value: false)]
                            maven_success = (m.result == "SUCCESS")
                        }
                )
                if (bazel_success && maven_success) {
                    build job: compare, wait: false
                }
            }
            return true
        }
        count = count + 1
        migrate_repo[it] = seq
    }
    parallel migrate_repo
}


@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {
        it.description.startsWith("Migration")
    }.collect { it.name }
}