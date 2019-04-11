import jenkins.*

enum Collision{
    exclusions, versionsMajor, versionsMinor, versionsIncremental, versionsQualifier
}

class CollisionCounts {
    Integer exclusionsCount = 0
    Integer versionsMajorCount = 0
    Integer versionsMinorCount = 0
    Integer versionsIncrementalCount = 0
    Integer versionsQualifierCount = 0

    def incrementCollision(Object match, Collision collision) {
        if (match.size() > 0) {
            switch (collision) {
                case Collision.exclusions:
                    exclusionsCount += match[0][1].toInteger()
                    break
                case Collision.versionsMajor:
                    versionsMajorCount += match[0][1].toInteger()
                    break
                case Collision.versionsMinor:
                    versionsMinorCount += match[0][1].toInteger()
                    break
                case Collision.versionsIncremental:
                    versionsIncrementalCount += match[0][1].toInteger()
                    break
                case Collision.versionsQualifier:
                    versionsQualifierCount += match[0][1].toInteger()
                    break
            }
        }
    }
}

node {
    // run in jenkins script console

    def folders = all_folders()

    def dep_conflict_job_name = "/DEBUG-print-dependency-collector-conflicts"

    def regular_collisions = new CollisionCounts()
    def all_deps_collisions = new CollisionCounts()
    def direct_collisions = new CollisionCounts()

    def successful_conflict_run_jobs = 0


    folders.each {
        def dep_conflict_run = Jenkins.instance.getItemByFullName(it + dep_conflict_job_name).lastSuccessfulBuild

        if (dep_conflict_run != null) {
            successful_conflict_run_jobs += 1
            def log = dep_conflict_run.log

            def match = log =~ /1-regular \| EXCLUSIONS count: (\d+)/
            regular_collisions.incrementCollision(match, Collision.exclusions)
            match = log =~ /2-all-deps \| EXCLUSIONS count: (\d+)/
            all_deps_collisions.incrementCollision(match, Collision.exclusions)
            match = log =~ /3-all-deps-direct \| EXCLUSIONS count: (\d+)/
            direct_collisions.incrementCollision(match, Collision.exclusions)

            match = log =~ /1-regular \| VERSIONS-MAJOR count: (\d+)/
            regular_collisions.incrementCollision(match, Collision.versionsMajor)
            match = log =~ /2-all-deps \| VERSIONS-MAJOR count: (\d+)/
            all_deps_collisions.incrementCollision(match, Collision.versionsMajor)
            match = log =~ /3-all-deps-direct \| VERSIONS-MAJOR count: (\d+)/
            direct_collisions.incrementCollision(match, Collision.versionsMajor)

            match = log =~ /1-regular \| VERSIONS-MINOR count: (\d+)/
            regular_collisions.incrementCollision(match, Collision.versionsMinor)
            match = log =~ /2-all-deps \| VERSIONS-MINOR count: (\d+)/
            all_deps_collisions.incrementCollision(match, Collision.versionsMinor)
            match = log =~ /3-all-deps-direct \| VERSIONS-MINOR count: (\d+)/
            direct_collisions.incrementCollision(match, Collision.versionsMinor)

            match = log =~ /1-regular \| VERSIONS-INCREMENTAL count: (\d+)/
            regular_collisions.incrementCollision(match, Collision.versionsIncremental)
            match = log =~ /2-all-deps \| VERSIONS-INCREMENTAL count: (\d+)/
            all_deps_collisions.incrementCollision(match, Collision.versionsIncremental)
            match = log =~ /3-all-deps-direct \| VERSIONS-INCREMENTAL count: (\d+)/
            direct_collisions.incrementCollision(match, Collision.versionsIncremental)

            match = log =~ /1-regular \| VERSIONS-QUALIFIER count: (\d+)/
            regular_collisions.incrementCollision(match, Collision.versionsQualifier)
            match = log =~ /2-all-deps \| VERSIONS-QUALIFIER count: (\d+)/
            all_deps_collisions.incrementCollision(match, Collision.versionsQualifier)
            match = log =~ /3-all-deps-direct \| VERSIONS-QUALIFIER count: (\d+)/
            direct_collisions.incrementCollision(match, Collision.versionsQualifier)
        }
    }

    def res =  """```
    |Total folders ${folders.size}
    |Total successful folders ${successful_conflict_run_jobs}
    |=======
    |EXCLUSIONS
    | - REGULAR         = ${regular_collisions.exclusionsCount}
    | - ALL_DEPS        = ${all_deps_collisions.exclusionsCount}
    | - ALL_DEPS_DIRECT = ${direct_collisions.exclusionsCount}
    |----------
    |VERSION_MAJOR
    | - REGULAR         = ${regular_collisions.versionsMajorCount}
    | - ALL_DEPS        = ${all_deps_collisions.versionsMajorCount}
    | - ALL_DEPS_DIRECT = ${direct_collisions.versionsMajorCount}
    |----------
    |VERSION_MINOR
    | - REGULAR         = ${regular_collisions.versionsMinorCount}
    | - ALL_DEPS        = ${all_deps_collisions.versionsMinorCount}
    | - ALL_DEPS_DIRECT = ${direct_collisions.versionsMinorCount}
    |----------
    |VERSION_INCREMENTAL
    | - REGULAR         = ${regular_collisions.versionsIncrementalCount}
    | - ALL_DEPS        = ${all_deps_collisions.versionsIncrementalCount}
    | - ALL_DEPS_DIRECT = ${direct_collisions.versionsIncrementalCount}
    |----------
    |QUALIFIER
    | - REGULAR         = ${regular_collisions.versionsQualifierCount}
    | - ALL_DEPS        = ${all_deps_collisions.versionsQualifierCount}
    | - ALL_DEPS_DIRECT = ${direct_collisions.versionsQualifierCount}
    |----------
    |```""".stripMargin()

    echo res
//    slackSend channel: "#bazel-mig-reports", message: res

}

@NonCPS
def all_folders() {
    Jenkins.instance.getItems(com.cloudbees.hudson.plugins.folder.Folder).findAll {
        it.description.startsWith("Migration")
    }.collect { it.name }
}