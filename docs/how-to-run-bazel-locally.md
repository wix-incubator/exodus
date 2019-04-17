# How to Run Exodus Locally

You can run the Exodus migration locally or on [Jenkins](how-to-run-migration-jenkins.md). 

Here's the info you need to run it locally.

### Clone the Exodus repo

Clone the following repository:
https://github.com/wix-incubator/exodus

### Build the Exodus migration CLI

Use this command line:  
```
cd [exodus path]
bazel build //migrator/wix-bazel-migrator:migrator_cli_deploy.jar
```

### Perform your code analysis
You can use either the Zinc Maven plugin, an open source option, or Codota, which is a licensed product. For details on each, review the [prerequisites](prerequisites.md). 

### Build your target Maven repository

```
$ cd <path to your Maven target repo>
$ mvn clean install
```
Running your Maven build populates the local .m2 repository that is used by Exodus to understand the structure of the build modules.
If you chose Zinc to analyze the code, the Maven output includes the dependency analysis used by Exodus to create the Bazel targets.


### Run Exodus

Run this command line in the directory where you cloned the Exodus repo.
Be sure to replace the following path locations where indicated:
* Path to the local .m2 repository
* Path to the target repository
Also change the `Drepo.url` to your target repository.

```
$ java -Xmx12G -Dskip.classpath=false -Dskip.transformation=false -Dlocal.maven.repository.path=<PATH-TO-LOCAL> .m2 REPO  -Dfail.on.severe.conflicts=true -Drepo.root=<TARGET-REPO> -Drepo.url=git@github.com:YOUR-ORG/target-repo.git -jar bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar
```
