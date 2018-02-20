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

    def compare_same = 0
    def compare_different = 0
    def compare_missing = 0
    def compare_overridden = 0

    def remote_run_success = 0
    def remote_run_fail = 0
    def remote_run_unstable = 0
    def remote_run_never_run = 0


    def recent = { run ->
        if (run == null)
            return false
        def d = run.time
        def yesterday = (new Date()).minus(1)
        return d.after(yesterday)
    }

    folders.each {
        def compare_run = Jenkins.instance.getItemByFullName(it + compare_job_name).lastCompletedBuild
        def bazel_run_run = Jenkins.instance.getItemByFullName(it + bazel_run_job_name).lastCompletedBuild
        def migrate_run = Jenkins.instance.getItemByFullName(it + migrate_job_name).lastCompletedBuild
        def bazel_remote_run = Jenkins.instance.getItemByFullName(it + remote_job_name).lastCompletedBuild


        if (recent(compare_run)) {
            if (compare_run.result == Result.SUCCESS)
                compare_success = compare_success + 1
            else
                compare_fail = compare_fail + 1

            def log = compare_run.log
            def match = log =~ /same: (\d+), different: (\d+), missing: (\d+), overridden: (\d+)/
            try {
                if (match.size() > 0) {
                    compare_same       += match[0][1].toInteger()
                    compare_different  += match[0][2].toInteger()
                    compare_missing    += match[0][3].toInteger()
                    compare_overridden += match[0][4].toInteger()
                }
            } catch (Exception ex) {
                println ex
            }
        } else {
            compare_never_run += 1
        }

        if (recent(bazel_run_run)) {
            if (bazel_run_run.result == Result.SUCCESS)
                run_success = run_success + 1
            else if (bazel_run_run.result == Result.UNSTABLE)
                run_unstable = run_unstable + 1
            else
                run_failure = run_failure + 1
        } else
            run_never_run = run_never_run + 1

        if (recent(migrate_run)) {
            if (migrate_run.result == Result.SUCCESS)
                migrate_success = migrate_success + 1
            else
                migrate_failure = migrate_failure + 1
        } else
            migrate_never_run = migrate_never_run + 1

        if (recent(bazel_remote_run)) {
            if (bazel_remote_run.result == Result.SUCCESS)
                remote_run_success = remote_run_success + 1
            else if (bazel_remote_run.result == Result.UNSTABLE)
                remote_run_unstable = remote_run_unstable + 1
            else
                remote_run_fail = remote_run_fail + 1
        } else
            remote_run_never_run = remote_run_never_run + 1
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
    | - total tests in maven: 
    | - total same = ${compare_same}
    | - total different = ${compare_different}
    | - total missing = ${compare_missing}
    | - total overridden = ${compare_overridden}
    |--
    |REMOTE
    | - success = ${remote_run_success}
    | - unstable = ${remote_run_unstable}
    | - failure = ${remote_run_fail}
    | - not-run = ${remote_run_never_run}
    |```""".stripMargin()

    echo res
    // slackSend channel: "#bazel-mig-reports", message: res

}


@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {
        it.description.startsWith("Migration")
    }.collect { it.name }
}