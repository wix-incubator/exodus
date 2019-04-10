// run in jenkins script console

def instance = Jenkins.instance
def folders = instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {it.description.startsWith("Migration") }.collect { it.name }

def migrate_job_name = "/01-migrate"
def bazel_run_job_name = "/05-run-bazel-rbe"

for (folder in folders){
    def migrate_run = instance.getItemByFullName(folder + migrate_job_name).lastSuccessfulBuild
    if (migrate_run == null) {
        continue
    }
    def migrate_branch = "bazel-mig-${migrate_run.number}"
    def params = new StringParameterValue('BRANCH_NAME', migrate_branch)
    def paramsAction = new ParametersAction(params)
    def bazel_run = instance.getItemByFullName(folder + bazel_run_job_name)
    instance.queue.schedule2(bazel_run,0,paramsAction)
}

""