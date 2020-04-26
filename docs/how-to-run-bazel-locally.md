# How to Run Exodus Locally

You can run the Exodus migration locally or on [Jenkins](how-to-run-migration-jenkins.md). 

Here's the info you need to run it locally.

### Download the latest release

1. go to releases page: <a href="https://github.com/wix/exodus/releases">https://github.com/wix/exodus/releases</a>

2. download the latest release exodus.jar - e.g. <a href="https://github.com/wix/exodus/releases/download/v0.1/exodus.jar">https://github.com/wix/exodus/releases/download/v0.1/exodus.jar</a>

#### You can also build Exodus from scratch:

1. Clone the following repository:
<a href="https://github.com/wix/exodus">https://github.com/wix/exodus</a>

2. Use this command line to build:  
```
cd <PATH TO YOUR LOCAL EXODUS REPO>
bazel build //migrator/wix-bazel-migrator:migrator_cli_deploy.jar
```

### Build your target Maven repository

```
$ cd <PATH TO YOUR MAVEN TARGET REPO>
$ mvn clean install
```
Running your Maven build populates the local .m2 repository and `target` folders that are used by Exodus to understand the structure of the build modules and the internal dependency graph.

### Run Exodus

Be sure to replace the following path locations where indicated:
* Path to the local .m2 repository
* Path to the target repository

Also change the `Drepo.url` to your target repository.

```
$ java -Xmx12G -Dskip.classpath=false -Dskip.transformation=false -Dlocal.maven.repository.path=<PATH-TO-LOCAL-.m2-REPO>  -Dfail.on.severe.conflicts=true -Drepo.root=<TARGET-REPO> -Drepo.url=git@github.com:YOUR-ORG/target-repo.git -jar <path/to/downloads>/exodus.jar
```

* If you've built exodus from scratch change <path/to/downloads>/exodus.jar to bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar to the download and run the command line in the directory where you cloned the Exodus repo.

* make sure to use FULL absolute paths for `-Dlocal.maven.repository.path` and `-Drepo.root`
