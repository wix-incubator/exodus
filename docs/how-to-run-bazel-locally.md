# How to Run Exodus Locally

You can run the Exodus migration locally or on [Jenkins](how-to-run-migration-jenkins.md). 
Here's the info you need to run it locally.

### Clone the repo

Clone the following repository:
https://github.com/wix-incubator/exodus

### Build the Exodus migration project

Use this command line:  `bazel build //migrator/wix-bazel-migrator:migrator_cli_deploy.jar`

### Build your target Maven repository
<Anything to say here?>

### Zinc / Codota
<Add content here.>

### Run XXXX

Run this command line:

```
 java -Xmx12G -Dcodota.token=<YOUR TOKEN> -Dartifactory.token=<YOUR TOKEN> -Dskip.classpath=false -Dskip.transformation=false -Dmanaged.deps.repo=<<path to core-server-build-ttols>> -Dlocal.maven.repository.path=<<path to ~/.m2/repository>>  -Dfail.on.severe.conflicts=true -Drepo.root=<<okhttp path>> -Drepo.url=git@github.com:square/okhttp.git -jar bazel-bin/migrator/wix-bazel-migrator/migrator_cli_deploy.jar
```
