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

    def total_maven = 0
    def total_bazel = 0

    def remote_run_success = 0
    def remote_run_fail = 0
    def remote_run_unstable = 0
    def remote_run_never_run = 0

    def migrated_tests = 0
    def compiled_tests = 0
    def passing_tests = 0
    def compared_tests = 0

    def compare_same = 0
    def compare_different = 0
    def compare_missing = 0
    def compare_overridden = 0
    def compare_ignored = 0
    def compare_failed = 0

    def latest_uber_job = last_ubered_build().getLastBuild().number
    echo "Latest Uber build#: $latest_uber_job"

    folders.each {
        def maven_tests = 0

        def compare_run      = get_latest_run(it + compare_job_name, latest_uber_job)
        def bazel_run_run    = get_latest_run(it + bazel_run_job_name, latest_uber_job)
        def migrate_run      = get_latest_run(it + migrate_job_name, latest_uber_job)
        def bazel_remote_run = get_latest_run(it + remote_job_name, latest_uber_job)

        def migrated = false
        def compiled = false
        def bazel_run = false
        def compare = false

        if (compare_run != null) {
            def log = compare_run.log
            def match = log =~ />>>> Total Maven Test Cases: (\d+)/
            if (match.size() > 0) {
                maven_tests = match[0][1].toInteger()
                println("Discovered maven tests for ${it}: " + maven_tests)
            }
            match = log =~ /> bazel cases: (\d+)/
            if (match.size() > 0) {
                total_bazel += match[0][1].toInteger()
            }
            match = log =~ /> same: (\d+), different: (\d+), missing: (\d+), overridden: (\d+), ignored: (\d+), failed: (\d+)/
            if (match.size() > 0) {
                compare_same       += match[0][1].toInteger()
                compare_different  += match[0][2].toInteger()
                compare_missing    += match[0][3].toInteger()
                compare_overridden += match[0][4].toInteger()
                compare_ignored    += match[0][5].toInteger()
                compare_failed     += match[0][6].toInteger()
            }

            if (compare_run.result == Result.SUCCESS) {
                compare_success += 1
                compare = true
            }
            else
                compare_fail += 1
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

        total_maven += maven_tests
        migrated_tests += migrated ? maven_tests : 0
        compiled_tests += compiled ? maven_tests : 0
        passing_tests  += bazel_run ? maven_tests : 0
        compared_tests += compare ? maven_tests : 0
    }
    def res =  """```
    |Total ${folders.size} for Uber ${latest_uber_job}
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
    | TEST TOTALS
    | - # Maven     = ${total_maven} [total # of maven tests that ran in a project]
    | - # Migrated  = ${migrated_tests} [... that belong to a migrated project]
    | - # Compiled  = ${compiled_tests} [... that belong to a project which is compiling in bazel]
    | - # Passing   = ${passing_tests} [... that belong to a project whose tests are passing in bazel]
    | - # Compared  = ${compared_tests} [... that belong to a project that passed comparison with maven]
    | ---- Total bazel tests: ${total_bazel}
    | ----   of them, failed: ${compare_failed}
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