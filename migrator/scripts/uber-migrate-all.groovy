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
        def run_sandbox = name + "/run-bazel-sandboxed"
        def delay = "${env.delay_between_trigger}".toInteger()
        def sleep_time = count * delay
        def seq = {
            sh "sleep $sleep_time"
            echo "[$name] START"
            def git_commit_hash = sh(script: "git ls-remote -q ${repo} | head -1 | cut -f 1", returnStdout: true)
            parallel(
                    "bazel-$name": {
                        def migration_branch = "master"
                        def bazel_run_number = 0
                        def bazel_success = false
                        echo "[$name] Git commit hash: ${git_commit_hash}"
                        def migrate_run = build job: migrate, wait: true, propagate: false,
                                parameters: [booleanParam(name: 'TRIGGER_BUILD', value: false), string(name: 'COMMIT_HASH', value: git_commit_hash)]
                        if (migrate_run.result == "SUCCESS") {
                            migration_branch = "bazel-mig-${migrate_run.number}"
                            def parameters = [
                                    string(name: 'BRANCH_NAME', value: migration_branch),
                                    booleanParam(name: 'CLEAN', value: false),
                                    string(name: 'COMMIT_HASH', value: git_commit_hash)
                            ]
                            build job: fix_deps, wait: true, propagate: false, parameters: parameters + booleanParam(name: 'TRIGGER_BUILD', value: false)
                            build job: run_rbe, wait: false, propagate: false, parameters: parameters
                            build job: run_sandbox, wait: false, propagate: false, parameters: parameters
                            def bazel_run = build job: run_bazel, wait: true, propagate: false, parameters: parameters
                            bazel_run_number = bazel_run.number
                            if (bazel_run.result == "SUCCESS"){
                                bazel_success = true
                            }
                        }
                        dir(name){
                            writeFile file:"bazel_success", text: "$bazel_success"
                            writeFile file:"migration_branch", text: migration_branch
                            writeFile file:"bazel_run_number", text: "$bazel_run_number"
                        }
                    },
                    "maven-$name": {
                        def maven_run = build job: run_maven, wait: true, propagate: false,
                                parameters: [string(name: 'COMMIT_HASH', value: git_commit_hash), booleanParam(name: 'CLEAN', value: false)]
                        dir(name){
                            writeFile file:"maven_run_number", text: "${maven_run.number}"
                            writeFile file:"maven_success", text: "${maven_run.result == "SUCCESS"}"
                        }
                    }
            )
            dir(name){
                def params = [
                            string(name: "ALLOW_MERGE", value: readFile("bazel_success")),
                            string(name: "MAVEN_SUCCESS", value: readFile("maven_success")),
                            string(name: 'BAZEL_RUN_NUMBER', value: readFile("bazel_run_number")),
                            string(name: 'MAVEN_RUN_NUMBER', value : readFile("maven_run_number")),
                            string(name: 'BRANCH_NAME', value : readFile("migration_branch"))
                        ]
                echo "[$name] ALLOW_MERGE=${readFile("bazel_success")}"
                echo "[$name] MAVEN_SUCCESS=${readFile("maven_success")}"
                echo "[$name] BAZEL_RUN_NUMBER=${readFile("bazel_run_number")}"
                echo "[$name] MAVEN_RUN_NUMBER=${readFile("maven_run_number")}"
                echo "[$name] BRANCH_NAME=${readFile("migration_branch")}"
                build job: compare, wait: false, parameters: params
                echo "[$name] FINISH"
            }
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