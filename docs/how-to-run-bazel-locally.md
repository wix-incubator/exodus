# How to Run Exodus Locally

You can run the Exodus migration locally or on [Jenkins](how-to-run-migration-jenkins.md). 

Here's the info you need to run it locally.

### Download the latest release

1. go to releases page: [https://github.com/wix/exodus/releases](https://github.com/wix/exodus/releases)

2. download the latest release exodus.jar - e.g. [https://github.com/wix/exodus/releases/download/v0.1/exodus.jar](https://github.com/wix/exodus/releases/download/v0.1/exodus.jar)

#### You can also build Exodus from scratch:

1. Clone the following repository:
[https://github.com/wix/exodus](https://github.com/wix/exodus)

2. Use this command line to build:  
```
cd <PATH TO YOUR LOCAL EXODUS REPO>
bazel build //migrator/wix-bazel-migrator:migrator_cli_deploy.jar
```

### Perform your code analysis
You can use either the Zinc Maven plugin, an open source option, or Codota, which is a licensed product. For details on each, review the [prerequisites](prerequisites.md). 

### Build your target Maven repository

```
$ cd <PATH TO YOUR MAVEN TARGET REPO>
$ mvn clean install
```
Running your Maven build populates the local .m2 repository that is used by Exodus to understand the structure of the build modules.
If you chose Zinc to analyze the code, the Maven output includes the dependency analysis used by Exodus to create the Bazel targets.


### Run Exodus

Be sure to replace the following path locations where indicated:
* Path to the local .m2 repository
* Path to the target repository

Also change the `Drepo.url` to your target repository.

```
$ java -Xmx12G -Dskip.classpath=false -Dskip.transformation=false -Dlocal.maven.repository.path=<PATH-TO-LOCAL> .m2 REPO  -Dfail.on.severe.conflicts=true -Drepo.root=<TARGET-REPO> -Drepo.url=git@github.com:YOUR-ORG/target-repo.git -jar <path/to/downloads>/exodus.jar
```

* If you've built exodus from scratch change <path/to/downloads>/exodus.jar to bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar to the donwload and run the command line in the directory where you cloned the Exodus repo.
