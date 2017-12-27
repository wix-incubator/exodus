pipeline {
    agent any
    options {
        timestamps()
    }
    environment {
        CODOTA_TOKEN = credentials("codota-token")
        REPO_NAME = find_repo_name()
        BRANCH_NAME = "bazel-mig-${env.BUILD_ID}"
    }
    stages {
        stage('checkout') {
            steps {
                dir("wix-bazel-migrator") {
                    copyArtifacts flatten: true, projectName: '../Migrator-build', selector: lastSuccessful()
                    script{
                        try{
                            copyArtifacts flatten: true, projectName: '01-migrate', selector: lastSuccessful()
                            sh "tar -xzf classpathModules.cache.tar.gz"
                        } catch(Exception e){
                            echo "could not copy artifacts"
                        }
                    }
                }
                dir("${env.REPO_NAME}") {
                    git "${env.repo_url}"
                }
            }
        }
        stage('debug') {
            steps {
                dir("wix-bazel-migrator") {
                    sh """|java -Xmx12G \\
                          |   -Dcodota.token=${env.CODOTA_TOKEN} \\
                          |   -Dskip.classpath=${env.skip_classpath} \\
                          |   -Dskip.transformation=false \\
                          |   ${env.EXTRA_DEPS} \\
                          |   -Dfail.on.severe.conflicts=true \\
                          |   -Drepo.root=../${repo_name}  \\
                          |   -cp wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar \\
                          |   ${env.main_class}""".stripMargin()
                }
            }
        }
    }
}

@SuppressWarnings("GroovyAssignabilityCheck")
def find_repo_name() {
    name = "${env.repo_url}".split('/')[-1]
    if (name.endsWith(".git"))
        name = name[0..-5]
    return name
}
