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
        def compare_rbe = name + "/03-compare-rbe"
        def compare_sandbox = name + "/03-compare-sandbox"
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

                        def rbe_run_number = 0
                        def rbe_success = false

                        def sandbox_run_number = 0
                        def sandbox_success = false

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

                            def bazel_jobs = [:]

                            bazel_jobs["rbe"]     = { build job: run_rbe, propagate: false, parameters: parameters }
                            bazel_jobs["sandbox"] = { build job: run_sandbox, propagate: false, parameters: parameters }
                            bazel_jobs["local"]   = { build job: run_bazel, propagate: false, parameters: parameters }
                            parallel bazel_jobs
                            def bazel_run = bazel_jobs["local"]
                            bazel_run_number = bazel_run.number
                            if (bazel_run.result == "SUCCESS"){
                                bazel_success = true
                            }
                            def rbe_run = bazel_jobs["rbe"]
                            rbe_run_number = rbe_run.number
                            if (rbe_run.result == "SUCCESS"){
                                rbe_success = true
                            }
                            def sandbox_run = bazel_jobs["sandbox"]
                            sandbox_run_number = sandbox_run.number
                            if (sandbox_run.result == "SUCCESS"){
                                sandbox_success = true
                            }


                        }
                        dir(name){
                            writeFile file:"migration_branch", text: migration_branch
                            writeFile file:"bazel_success", text: "$bazel_success"
                            writeFile file:"bazel_run_number", text: "$bazel_run_number"
                            writeFile file:"rbe_success", text: "$rbe_success"
                            writeFile file:"rbe_run_number", text: "$rbe_run_number"
                            writeFile file:"sandbox_success", text: "$sandbox_success"
                            writeFile file:"sandbox_run_number", text: "$sandbox_run_number"
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
                    string(name: "MAVEN_SUCCESS", value: readFile("maven_success")),
                    string(name: 'MAVEN_RUN_NUMBER', value : readFile("maven_run_number")),
                    string(name: 'BRANCH_NAME', value : readFile("migration_branch")),
                ]

                run_compare(compare, params + [
                    string(name: "ALLOW_MERGE", value: readFile("bazel_success")),
                    string(name: 'BAZEL_RUN_NUMBER', value: readFile("bazel_run_number")),
                    string(name: 'BAZEL_COMPARE_JOB', value: "02-run-bazel")
                ])
                run_compare(compare_rbe, params + [
                    string(name: "ALLOW_MERGE", value: readFile("rbe_success")),
                    string(name: 'BAZEL_RUN_NUMBER', value: readFile("rbe_run_number")),
                    string(name: 'BAZEL_COMPARE_JOB', value: "05-run-bazel-rbe")
                ])
                run_compare(compare_sandbox, params + [
                    string(name: "ALLOW_MERGE", value: readFile("sandbox_success")),
                    string(name: 'BAZEL_RUN_NUMBER', value: readFile("sandbox_run_number")),
                    string(name: 'BAZEL_COMPARE_JOB', value: "run-bazel-sandboxed")
                ])

                echo "[$name] FINISH"
            }
            return true
        }
        count += 1
        migrate_repo[name] = seq
    }
    parallel migrate_repo
}

def run_compare(jobname, params) {
    echo params
    build job: jobname, wait: false, parameters: params




}

@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {
        it.description.startsWith("Migration")
    }.collect {
        [job_name: it.name, repo: it.description.substring("Migration to ".length())]
    }
}