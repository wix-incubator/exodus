pipeline {
    agent any
    options {
        timestamps()
    }
    environment {
        CODOTA_TOKEN = credentials("codota-token")
        REPO_NAME = find_repo_name()
        MANAGED_DEPS_REPO_URL = "git@github.com:wix-private/core-server-build-tools.git"
        MANAGED_DEPS_REPO_NAME = "core-server-build-tools"
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
                dir("${env.MANAGED_DEPS_REPO_NAME}") {
                    checkout([$class: 'GitSCM', branches: [[name: 'master' ]],
                              userRemoteConfigs: [[url: "${env.MANAGED_DEPS_REPO_URL}"]]])
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
                          |   -Dmanaged.deps.repo=../${env.MANAGED_DEPS_REPO_NAME} \\
                          |   ${env.EXTRA_DEPS} \\
                          |   -Dfail.on.severe.conflicts=true \\
                          |   -Drepo.root=../${repo_name}  \\
                          |   -Drepo.url=${env.repo_url} \\
                          |   -cp wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar \\
                          |   ${env.main_class}""".stripMargin()
                }
            }
        }
    }
    post {
        always {
            deleteDir()
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
