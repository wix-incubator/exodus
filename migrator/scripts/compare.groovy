pipeline {
    agent any
    stages {
        stage('maven artifacts') {
            steps {
                script {
                    try {
                        copyArtifacts projectName: '02-run-maven', target: 'maven-output', selector: lastCompleted()
                    } catch (err) {
                        echo "[WARN] unable to copy maven artifacts, perhaps none exist?"
                    }
                }
            }
        }
        stage('bazel artifacts') {
            steps {
                script {
                    try {
                        copyArtifacts projectName: '02-run-bazel', target: 'bazel-output', selector: lastCompleted()
                    } catch (err) {
                        echo "[WARN] unable to copy bazel artifacts, perhaps none exist?"
                    }
                }
            }
        }
        stage('compare') {
            steps {
                script {
                    if (!has_artifacts('bazel') && !has_artifacts('maven')) {
                        echo "No tests were detected in both maven and bazel"
                        currentBuild.result = 'UNSTABLE'
                        return
                    }
                    
                    dir('core-server-build-tools') {
                        git "git@github.com:wix-private/core-server-build-tools.git"
                        ansiColor('xterm') {
                            sh """|export PYTHONIOENCODING=UTF-8
                                  |cd scripts
                                  |pip3 install --user -r requirements.txt
                                  |python3 -u maven_bazel_diff.py maven-count ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                                  |python3 -u maven_bazel_diff.py compare ${WORKSPACE}/maven-output ${WORKSPACE}/bazel-output
                                  |""".stripMargin()
                        }
                    }
                }
            }
        }
    }
}

def has_artifacts(dir) {
    return findFiles(glob: "${dir}-output/**/*.xml").any()
}

