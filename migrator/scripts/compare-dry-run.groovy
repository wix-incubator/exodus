// run in jenkins script console

def instance = Jenkins.instance
def folders = instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {it.description.startsWith("Migration") }.collect { it.name }

def dry_run_job_name = "/DEBUG-migrate-dry-run"
def migrate_job_name = "/01-migrate"
def bazel_run_job_name = "/02-run-bazel"


def success = 0
def success_new = 0
def unstable = 0
def unstable_new = 0
def fail = 0
def fail_new = 0
def failed = []

folders.each{
    job = instance.getItemByFullName(it + dry_run_job_name)
    migrate_job = instance.getItemByFullName(it + migrate_job_name)
    bazel_run_job = instance.getItemByFullName(it + bazel_run_job_name)
    last_bazel_run = bazel_run_job.lastCompletedBuild
    lastRun = job.lastCompletedBuild
    if (lastRun == null){
        not_migrated = not_migrated + 1
    } else if (lastRun.result == Result.SUCCESS){
        if (last_bazel_run == null || last_bazel_run.result != Result.SUCCESS)
            success_new = success_new + 1
        success = success + 1
    } else if (lastRun.result == Result.UNSTABLE) {
        if (last_bazel_run != null && last_bazel_run.result == Result.SUCCESS)
            unstable_new = unstable_new + 1
        unstable = unstable + 1
    } else if (lastRun.result == Result.FAILURE) {
        if (last_bazel_run != null && (last_bazel_run.result == Result.SUCCESS || last_bazel_run.result == Result.UNSTABLE)) {
            fail_new = fail_new + 1
            failed.add(lastRun.url)
        }
        fail = fail + 1
    }
}

println "Total ${folders.size}:"
println "======="
println "success = ${success}"
println "success_new = ${success_new}"
println "--"
println "unstable = ${unstable}"
println "unstable_new = ${unstable_new}"
println "--"
println "fail = ${fail}"
println "fail_new = ${fail_new}"
println ""
failed.each{println(instance.rootUrl + it)}
""