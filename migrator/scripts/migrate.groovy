pipeline {
    agent any
    options {
        timestamps()
        throttle(categories: ['migrate'])
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
                }
                dir("${env.REPO_NAME}") {
                    git "${env.repo_url}"
                }
            }
        }
        stage('migrate') {
            steps {
                dir("${env.REPO_NAME}") {
                    sh 'rm -rf third_party'
                    sh 'find . -path "*/*BUILD" -exec rm -f {} \\;'
                }
                dir("wix-bazel-migrator") {
                    sh "java -Xmx12G -Dcodota.token=${env.CODOTA_TOKEN} -Dclean.codota.analysis.cache=true -Dskip.classpath=false -Dskip.transformation=false -Dfail.on.severe.conflicts=true -Drepo.root=../${repo_name} -jar wix-bazel-migrator-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
                }
            }
        }
        stage('buildifier') {
            steps {
                dir("${env.REPO_NAME}") {
                    sh "buildozer 'add tags manual' //third_party/...:%scala_import"
                    sh 'buildifier $(find . -iname BUILD -type f)'
                }
            }
        }
        stage('push-to-git') {
            steps {
                dir("${env.REPO_NAME}"){
                   sh """|git checkout -b ${env.BRANCH_NAME}
                         |git add .
                         |git commit -m "bazel migrator created by ${env.BUILD_URL}"
                         |git push origin ${env.BRANCH_NAME}
                         |""".stripMargin()
                }
            }
        }
    }
    post {
        always {
            script {
                try {
                    dir("wix-bazel-migrator") {
                        echo "[INFO] creating tar.gz files for migration artifacts..."
                        sh """|tar czf classpathModules.cache.tar.gz classpathModules.cache
                              |tar czf cache.tar.gz cache
                              |tar czf dag.bazel.tar.gz dag.bazel""".stripMargin()
                    }
                } catch (err) {
                    echo "[WARN] could not create all tar.gz files ${err}"
                } finally {
                    archiveArtifacts "wix-bazel-migrator/classpathModules.cache.tar.gz,wix-bazel-migrator/dag.bazel.tar.gz,wix-bazel-migrator/cache.tar.gz"
                }
            }
        }
        success{
            build job: "03-fix-strict-deps", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: true
            script{
                if ("${env.TRIGGER_BUILD}" != "false"){
                    build job: "02-run-bazel", parameters: [string(name: 'BRANCH_NAME', value: "${env.BRANCH_NAME}")], propagate: false, wait: false
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
