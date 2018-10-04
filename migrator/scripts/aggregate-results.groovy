import jenkins.*

node {
    // run in jenkins script console

    def folders = all_folders()

    def migrate_job_name = "/01-migrate"
    def bazel_run_job_name = "/02-run-bazel"
    def compare_job_name = "/03-compare"
    def compare_rbe_job_name = "/03-compare-rbe"
    def compare_sandbox_job_name = "/03-compare-sandbox"
    def remote_job_name = "/05-run-bazel-rbe"
    def sandbox_job_name = "/run-bazel-sandboxed"

    def compare_success = 0
    def compare_fail = 0
    def compare_never_run = 0

    def compare_success_rbe = 0
    def compare_fail_rbe = 0
    def compare_never_run_rbe = 0

    def compare_success_sandbox = 0
    def compare_fail_sandbox = 0
    def compare_never_run_sandbox = 0

    def run_success = 0
    def run_failure = 0
    def run_unstable = 0
    def run_never_run = 0

    def migrate_success = 0
    def migrate_failure = 0
    def migrate_never_run = 0

    def total_maven = 0
    def total_bazel = 0
    def total_bazel_rbe = 0
    def total_bazel_sandbox = 0

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

    def compare_same_rbe = 0
    def compare_different_rbe = 0
    def compare_missing_rbe = 0
    def compare_overridden_rbe = 0
    def compare_ignored_rbe = 0
    def compare_failed_rbe = 0

    def compare_same_sandbox = 0
    def compare_different_sandbox = 0
    def compare_missing_sandbox = 0
    def compare_overridden_sandbox = 0
    def compare_ignored_sandbox = 0
    def compare_failed_sandbox = 0

    def sandbox_run_success = 0
    def sandbox_run_fail = 0
    def sandbox_run_unstable = 0
    def sandbox_run_never_run = 0

    def latest_uber_job = last_ubered_build().getLastBuild().number
    echo "Latest Uber build#: $latest_uber_job"

    folders.each {
        def maven_tests = 0

        def compare_run         = get_latest_run(it + compare_job_name, latest_uber_job)
        def compare_run_rbe     = get_latest_run(it + compare_rbe_job_name, latest_uber_job)
        def compare_run_sandbox = get_latest_run(it + compare_sandbox_job_name, latest_uber_job)
        def bazel_run_run       = get_latest_run(it + bazel_run_job_name, latest_uber_job)
        def migrate_run         = get_latest_run(it + migrate_job_name, latest_uber_job)
        def bazel_remote_run    = get_latest_run(it + remote_job_name, latest_uber_job)
        def bazel_sandbox_run   = get_latest_run(it + sandbox_job_name, latest_uber_job)

        def migrated = false
        def compiled = false
        def bazel_run = false
        def compare = false
        def compare_rbe = false
        def compare_sandbox = false

        if (compare_run != null) {
            def match = compare_run.log =~ />>>> Total Maven Test Cases: (\d+)/
            if (match.size() > 0) {
                maven_tests = match[0][1].toInteger()
            }

            def data = extract_compare_data(compare_run.log)
            total_bazel        += data.bazel_tests
            compare_same       += data.compare_same
            compare_different  += data.compare_different
            compare_missing    += data.compare_missing
            compare_overridden += data.compare_overridden
            compare_ignored    += data.compare_ignored
            compare_failed     += data.compare_ignored

            if (compare_run.result == Result.SUCCESS) {
                compare_success += 1
                compare = true
            }
            else
                compare_fail += 1
        } else {
            compare_never_run += 1
        }

        // ugh, deduplicate this
        if (compare_run_rbe != null) {
            def data = extract_compare_data(compare_run_rbe.log)
            total_bazel_rbe        += data.bazel_tests
            compare_same_rbe       += data.compare_same
            compare_different_rbe  += data.compare_different
            compare_missing_rbe    += data.compare_missing
            compare_overridden_rbe += data.compare_overridden
            compare_ignored_rbe    += data.compare_ignored
            compare_failed_rbe     += data.compare_ignored

            if (compare_run_rbe.result == Result.SUCCESS) {
                compare_success_rbe += 1
                compare_rbe = true
            }
            else
                compare_fail_rbe += 1
        } else {
            compare_never_run_rbe += 1
        }

        if (compare_run_sandbox != null) {
            def data = extract_compare_data(compare_run_sandbox.log)
            total_bazel_sandbox        += data.bazel_tests
            compare_same_sandbox       += data.compare_same
            compare_different_sandbox  += data.compare_different
            compare_missing_sandbox    += data.compare_missing
            compare_overridden_sandbox += data.compare_overridden
            compare_ignored_sandbox    += data.compare_ignored
            compare_failed_sandbox     += data.compare_ignored

            if (compare_run_sandbox.result == Result.SUCCESS) {
                compare_success_sandbox += 1
                compare_sandbox = true
            }
            else
                compare_fail_sandbox += 1
        } else {
            compare_never_run_sandbox += 1
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

        if (bazel_sandbox_run != null) {
            if (bazel_sandbox_run.result == Result.SUCCESS)
                sandbox_run_success += 1
            else if (bazel_sandbox_run.result == Result.UNSTABLE)
                sandbox_run_unstable += 1
            else
                sandbox_run_fail += 1
        } else
            sandbox_run_never_run += 1

        total_maven += maven_tests
        migrated_tests += migrated ? maven_tests : 0
        compiled_tests += compiled ? maven_tests : 0
        passing_tests  += bazel_run ? maven_tests : 0
        compared_tests += compare ? maven_tests : 0
    }
    def res =  """```
    |Total ${folders.size} projects for Uber run# ${latest_uber_job}
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
    | TEST TOTALS (# of maven tests)
    | - # Maven     = ${total_maven} [that ran in a project]
    | - # Migrated  = ${round((migrated_tests*100)/total_maven)}% ${migrated_tests} [ran in a migrated project]
    | - # Compiled  = ${round((compiled_tests*100)/total_maven)}% ${compiled_tests} [project which is compiling in bazel]
    | - # Passing   = ${round((passing_tests*100)/total_maven)}% ${passing_tests} [project whose tests are passing in bazel]
    | - # Compared  = ${round((compared_tests*100)/total_maven)}% ${compared_tests} [project that passed comparison with maven]
    | ---- Total bazel tests: ${total_bazel}
    | ----   of them, passing: ${total_bazel - compare_failed}
    | ----   of them, failed: ${compare_failed}
    |--
    |REMOTE
    | - success = ${remote_run_success}
    | - unstable = ${remote_run_unstable}
    | - failure = ${remote_run_fail}
    | - not-run = ${remote_run_never_run}
    | ---- Total bazel tests: ${total_bazel_rbe}
    | ----   of them, passing: ${total_bazel_rbe - compare_failed_rbe}
    | ----   of them, failed: ${compare_failed_rbe}
    |--
    |SANDBOX
    | - success = ${sandbox_run_success}
    | - unstable = ${sandbox_run_unstable}
    | - failure = ${sandbox_run_fail}
    | - not-run = ${sandbox_run_never_run}
    | ---- Total bazel tests: ${total_bazel_sandbox}
    | ----   of them, passing: ${total_bazel_sandbox - compare_failed_sandbox}
    | ----   of them, failed: ${compare_failed_sandbox}
    |```""".stripMargin()

    echo res
    slackSend channel: "#bazel-mig-reports", message: res

}

@NonCPS
extract_compare_data(log) {
    def result = [
        bazel_tests: 0,
        compare_same:       0,
        compare_different:  0,
        compare_missing:    0,
        compare_overridden: 0,
        compare_ignored:    0,
        compare_failed:     0,
    ]
    def match = log =~ /> bazel cases: (\d+)/
    if (match.size() > 0) {
        result.bazel_tests = match[0][1].toInteger()
    }
    match = log =~ /> same: (\d+), different: (\d+), missing: (\d+), overridden: (\d+), ignored: (\d+), failed: (\d+)/
    if (match.size() >= 5) {
        result.compare_same       = match[0][1].toInteger()
        result.compare_different  = match[0][2].toInteger()
        result.compare_missing    = match[0][3].toInteger()
        result.compare_overridden = match[0][4].toInteger()
        result.compare_ignored    = match[0][5].toInteger()
        result.compare_failed     = match[0][6].toInteger()
    }
    return result
}

def round(amt) {
    amt.setScale(2, BigDecimal.ROUND_HALF_UP)
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