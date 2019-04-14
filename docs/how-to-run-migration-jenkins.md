# Run Migration on Jenkins
When migrating using Exodus, it is a good idea to use a build server such as Jenkins. The migration is a computational intensive operation and offloading this work to a build server allows you to continue working in your local environment while the migration is running. Another advantage is that a build server can help alleviate the grunt work of dealing with an iterative process such as this migration.

## Prerequisites
You'll need the following before running a migration on Jenkins.

### Jenkins Server Setup
- Jenkins LTS
- Install the following plugins:
    - [Pipeline](https://wiki.jenkins.io/display/JENKINS/Pipeline+Plugin)
    - [Timestamper](https://wiki.jenkins.io/display/JENKINS/Timestamper)
    - [AnsiColor](https://wiki.jenkins.io/display/JENKINS/AnsiColor+Plugin)
    - [Custom tool plugin](https://wiki.jenkins.io/display/JENKINS/Custom+Tools+Plugin)
    - [Git plugin](https://wiki.jenkins.io/display/JENKINS/Git+Plugin)
    - (Optional) [Slack](https://wiki.jenkins.io/display/JENKINS/Slack+Plugin)

### Install Bazel:
1. Open **"Global Tool Configuration**
2. Expand "Custom Tools"
3. Click **"Add Custom tool"**
4. Enter the following:

    - Name: `bazel`
    - Exported Paths: `bin`
    - Install automatically: yes
    - Choose **"Run Shell Command"** and enter the following shell command. (This example is for Bazel 0.24.1, edit your code to match your Bazel version).

        ```bash
        # Fetch the Bazel installer
        pwd
        BAZEL_VERSION=0.24.1
        BASE=$(pwd)/${BAZEL_VERSION}
        BAZEL_INSTALLER=${BASE}/install.sh
        URL=https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh
        if [ ! -f ${BASE}/bin/bazel ]; then
            mkdir -p ${BASE}
            curl -L -o ${BAZEL_INSTALLER} ${URL}
            # Install bazel inside ${BASE}
            bash "${BAZEL_INSTALLER}" \
            --prefix="${BASE}"
        else
            echo "BAZEL ${BAZEL_VERSION} is already installed"
        fi
        ```
   - Tool home: `0.24.0`

### Install Buildozer
Follow the same steps as above, but with the tool home and name `buildozer` and the following shell command. (This example is for Buildozer v0.20.0, edit your code to match your Buildozer version).

```bash
# Fetch the Bazel installer
pwd
VERSION=0.20.0
BASE=$(pwd)/buildozer
URL=https://github.com/bazelbuild/buildtools/releases/download/${VERSION}/buildozer
if [ ! -f ${BASE}/bin/buildozer ]; then
    mkdir -p ${BASE}/bin
    curl -L -o ${BASE}/bin/buildozer $URL
    chmod +x ${BASE}/bin/buildozer
    echo "buildozer is installed on ${BASE}/bin/buildozer"
else
    echo "buildozer is already installed"
fi
```

### Install buildifier
Follow the same steps as above, but with the tool home and name `buildifier` and the following shell command: (This example is for buildifier 0.20.0, edit your code to match your buildifier version).

```bash
# Fetch the Bazel installer
pwd
VERSION=0.20.0
BASE=$(pwd)/buildifier
URL=https://github.com/bazelbuild/buildtools/releases/download/${VERSION}/buildifier
if [ ! -f ${BASE}/bin/buildifier ]; then
    mkdir -p ${BASE}/bin
    curl -L -o ${BASE}/bin/buildifier -L $URL
    chmod +x ${BASE}/bin/buildifier
    echo "buildifier is installed on ${BASE}/bin/buildifier"
else
    echo "buildifier is already installed"
fi
```

### Build Pipeline

In the build pipeline add the following environment:

```groovy
environment {
        BAZEL_HOME = tool name: 'bazel', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool' // needs https://wiki.jenkins.io/display/JENKINS/Custom+Tools+Plugin
        BUILDOZER_HOME = tool name: 'buildozer', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        BUILDIFIER_HOME = tool name: 'buildifier', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        PATH = "$BAZEL_HOME/bin:$BUILDOZER_HOME/bin:$BUILDIFIER_HOME/bin:$JAVA_HOME/bin:$PATH"
    }
```

## Configure Jenkins Jobs

Configure the following Jenkins Jobs:

![Jenkins Jobs](assets/img/jenkins-jobs.png "Jenkins Jobs")



