import jenkins.*

node {
    // run in jenkins script console

    def folders = all_folders()

    def migrate_job_name = "/01-migrate"
    def bazel_run_job_name = "/02-run-bazel"
    def compare_job_name = "/03-compare"
    def remote_job_name = "/05-run-bazel-rbe"

    def compare_success = 0
    def compare_fail = 0
    def compare_never_run = 0

    def run_success = 0
    def run_failure = 0
    def run_unstable = 0
    def run_never_run = 0

    def migrate_success = 0
    def migrate_failure = 0
    def migrate_never_run = 0

    def compare_total_maven = 0
    def compare_total_bazel = 0

    def remote_run_success = 0
    def remote_run_fail = 0
    def remote_run_unstable = 0
    def remote_run_never_run = 0

    def migrated_tests = 0
    def compiled_tests = 0
    def run_tests = 0

    def latest_uber_job = last_ubered_build().getLastBuild().number
    echo "Latest Uber build#: $latest_uber_job"

    folders.each {
        def maven_tests = 0
        def bazel_tests = 0

        def compare_run      = get_latest_run(it + compare_job_name, latest_uber_job)
        def bazel_run_run    = get_latest_run(it + bazel_run_job_name, latest_uber_job)
        def migrate_run      = get_latest_run(it + migrate_job_name, latest_uber_job)
        def bazel_remote_run = get_latest_run(it + remote_job_name, latest_uber_job)

        def migrated = false
        def compiled = false
        def bazel_run = false

        if (compare_run != null) {
            if (compare_run.result == Result.SUCCESS)
                compare_success += 1
            else
                compare_fail += 1

            def log = compare_run.log
            def match = log =~ ">>>> Total Maven Test Cases: 8" =~ />>>> Total Maven Test Cases: (\d+)/
            if (match.size() > 0) {
                maven_tests = match[0][1].toInteger()
            }
            match = log =~ /> bazel cases: (\d+)/
            if (match.size() > 0) {
                bazel_tests = match[0][1].toInteger()
            }
        } else {
            compare_never_run += 1
        }

        if (bazel_run_run != null) {
            if (bazel_run_run.result == Result.SUCCESS) {
                bazel_run = true
                compiled = true
                run_success += 1
            } else if (bazel_run_run.result == Result.UNSTABLE) {
                compiled = true
                run_unstable += 1
            } else
                run_failure += 1
        } else
            run_never_run += 1

        if (migrate_run != null) {
            if (migrate_run.result == Result.SUCCESS) {
                migrated = true
                migrate_success += 1
            } else
                migrate_failure += 1
        } else
            migrate_never_run += 1

        if (bazel_remote_run != null) {
            if (bazel_remote_run.result == Result.SUCCESS)
                remote_run_success += 1
            else if (bazel_remote_run.result == Result.UNSTABLE)
                remote_run_unstable += 1
            else
                remote_run_fail += 1
        } else
            remote_run_never_run += 1

        compare_total_maven += maven_tests
        compare_total_bazel += bazel_run ? bazel_tests : 0
        migrated_tests += migrated ? maven_tests : 0
        compiled_tests += compiled ? maven_tests : 0
        run_tests += (migrated || compiled) ? maven_tests : 0
    }
    def res =  """```
    |Total ${folders.size}
    |=======
    |MIGRATION
    | - success = ${migrate_success}
    | - failure = ${migrate_failure}
    | - not-run = ${migrate_never_run}
    |--
    |BAZEL RUN
    | - success = ${run_success}
    | - unstable = ${run_unstable}
    | - failure = ${run_failure}
    | - not-run = ${run_never_run}
    |--
    |COMPARE
    | - success = ${compare_success}
    | - failure = ${compare_fail}
    | - not-run = ${compare_never_run}
    | -----
    | TOTALS
    | - # Maven = ${compare_total_maven} [total # of maven tests that ran in maven]
    | - # Migrated  = ${migrated_tests} [total # of tests that were migrated]
    | - # Compiled  = ${compiled_tests} [total # of tests that compiled]
    | - # Ran       = ${run_tests} [total # of tests that ran in maven]
    | - # Bazel Run = ${compare_total_bazel} [total # of tests that ran in bazel]
    |--
    |REMOTE
    | - success = ${remote_run_success}
    | - unstable = ${remote_run_unstable}
    | - failure = ${remote_run_fail}
    | - not-run = ${remote_run_never_run}
    |```""".stripMargin()

    echo res
    slackSend channel: "#bazel-mig-reports", message: res

}

@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder)
            .findAll { it.description.startsWith("Migration") }
            .collect { it.name }
}

@NonCPS
def last_ubered_build() {
    return Jenkins.instance.getItemByFullName("Uber-migrate-all")
}

@NonCPS
def get_latest_run(job_id, upstream_id) {
    Jenkins.instance.getItemByFullName(job_id).getBuilds().find {
        def cause = it.getCause(hudson.model.Cause$UpstreamCause)
        cause?.upstreamBuild == upstream_id &&
        cause?.upstreamProject == "Uber-migrate-all"
    }
}