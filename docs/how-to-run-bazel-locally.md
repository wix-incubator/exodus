# How to Run Exodus Locally

You can run the Exodus migration locally or on [Jenkins](how-to-run-migration-jenkins.md). 
Here's the info you need to run it locally.

### Clone the repo

Clone the following repository:
https://github.com/wix-incubator/exodus

### Build the Exodus migration cli

Use this command line:  
```
cd [exodus path]
bazel build //migrator/wix-bazel-migrator:migrator_cli_deploy.jar
```

### perform your code analysis
Zinc / Codota 
link to pre-requsites

### Build your target Maven repository
```
$ cd [target repo path]
$ mvn clean install
```
* this action populates local .m2 repository that is used by exodus to undestand the structure of the modules 
** if you chose zinc the maven output will include dependency analysis used by exodus to create bazel targets


### Run Exodus

Run this command line:

```
$ cd [exodus path]
$ java -Xmx12G -Dskip.classpath=false -Dskip.transformation=false -Dlocal.maven.repository.path=[path to local .m2 repository]  -Dfail.on.severe.conflicts=true -Drepo.root=[target-repo] -Drepo.url=git@github.com:your-org/target-repo.git -jar bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar
```
