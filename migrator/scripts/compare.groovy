pipeline {
    agent any
    options {
        timestamps()
    }
    stages {
        stage('maven artifacts') {
            steps {
                copyArtifacts('02-run-maven') {
                    includePatterns('*')
                    targetDirectory('maven-output')
                    buildSelector {
                        latestSuccessful(true)
                    }
                    optional()
                }
            }
        }
        stage('bazel artifacts') {
            steps {
                copyArtifacts('02-run-bazel') {
                    includePatterns('*')
                    targetDirectory('bazel-output')
                    buildSelector {
                        latestSuccessful(true)
                    }
                    optional()
                }
            }
        }
        stage('compare') {
            steps {
                if (!has_artifacts('bazel') || !has_artifacts('maven')) {
                    currentBuild.result = 'UNSTABLE'
                    return
                }
                sh """|export PYTHONIOENCODING=UTF-8
                      |cd scripts
                      |pip3 install --user -r requirements.txt
                      |python3 -u maven_bazel_diff.py ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                      |""".stripMargin()
            }
        }
    }
}

def has_artifacts(dir) {
    if (!findFiles(glob: "${WORKSPACE}/${dir}-output/**/*.xml").any()) {
        echo "[WARN] could not find ${dir} artifacts!"
    }
}