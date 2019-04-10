## Known Issues
0. [OOM errors](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/troubleshooting-sandbox-failures.md#oom-jenkins-errors) on jenkins `run-bazel-sandboxed' 
1. Any error to do with embedded Mysql / Mongo - see [here](troubleshooting-build-failures.md#checklist-for-test-target-that-uses-embeddedmysqlembeddedmongo)
2. Errors like `caused by FileSystem readonly` or `Could not create dir`. See [Filesystem write errors](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/troubleshooting-sandbox-failures.md#file-system-writes)
3. Errors like `com.wix.e2e.http.exceptions.ConnectionRefusedException: Unable to connect to port 9901` or `Caused by: java.net.UnknownHostException: fastdl.mongodb.org`, or any other ConnectionException. See [external network access](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/troubleshooting-sandbox-failures.md#external-network-access)
4. Erros like `java.net.BindException: Cannot assign requested address (Bind failed)`. See [hostname resolving](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/troubleshooting-sandbox-failures.md#host-name-resolving)

## What is Sandboxing?
One of the steps in the migration is run-bazel-sandboxed ([example](http://ci-jenkins-poc0a.42.wixprod.net/job/photography/job/run-bazel-sandboxed/58/consoleText))

This step runs all of your tests in the way that we would like to do in local development too.<br>
That means running with `bazel test //...` instead of separating unit tests and ITs like today:<br>
`bazel test --test_tag_filters=IT --strategy=TestRunner=standalone --jobs=1`

The ideal way to tell bazel to run all tests in your project is `bazel test //...`.<br>
This parallelizes the execution and run each test in a hermetic (i.e isolated, repeatable..) sandbox.<br>
As with all sandboxes, this affects two main aspects - filesystem and network interactions.

## Why can't we enjoy this awesomness locally?
* The reason we disable the sandbox (`--strategy=TestRunner=standalone`):<br>
bazel cannot reach full network isolation on mac (due to OSx limitation). <br>
This is a problem since, unfortunately, our IT and E2E tests use the same common ports (as opposed to randomized ports).<br>
* The reason we run sequentially (`--jobs=1`) - is due to a a few hermeticity problems we're investigating currently (sockets, hostname resolving and docker).

We're actively working on being able to develop locally with the full benefits of `bazel test //...`, most probably based on using docker-for-mac.

## I want to run only 1 step with sandboxing
* Go to *"debugging"* tab in your migration folder 
* Find the job *"Test or build partial project"*
* Enter branch name (usually `bazel-mig-X`)
* Enter full label of target or wildcard (`//foo/bar/...`)
* Select if you want to run `bazel build` or `bazel test`

Notes:<br>
* The job does not run migration - select a branch that has bazel in it
* Bazel would build anything needed for the targets you selected (even targets outside of the scope you selected)

## All my other steps are Green. Why do I have to make this sandbox step green too?
In the meantime, this `run-bazel-sandboxed` step in jenkins is the best way to make sure your code is prepared for local development.<br>
In addition, the actual ci builds on RBE also run in sandboxed mode.
RBE - Google "Remote Build Execution" - A flexible workers farm for extremely fast builds.
At the moment the network sandboxing there is not full, but that is expected to change in the near future and when this happens tests will fail there too in some cases.

## How can I see the log of a failed test?
- Navigate to a link like [this](http://ci-jenkins-poc0a.42.wixprod.net/job/ecom/job/run-bazel-sandboxed/16/artifact/) (subsitute your project name instead of 'ecom')<br>
Click the  link to “build artifacts” -> “bazel-out/k8-fastbuild/testlogs”

- example [link to specific test](http://ci-jenkins-poc0a.42.wixprod.net/job/ecom/job/run-bazel-sandboxed/16/artifact/bazel-out/k8-fastbuild/testlogs/commons-server/commons-encryption/commons-encryption-core/src/test/scala/com/wix/ecommerce/commons/commons/test.log) - there is a tiny “download” icon next to the run number in the build history table

## File System Writes
Errors such as: `caused by FileSystem readonly`, `Could not create dir`.<br>
Writing to non tmp destinations violates hermeticity.<br>
FileSystem interactions should be limited to the default java tmp dir, by using Files.createTempDirectory() or such, like [here](https://github.com/wix-platform/wix-framework/blob/f86fea548916977c01038bd9119d9b15dcbb3d32/koboshi-modules/wix-koboshi/src/test/java/com/wixpress/framework/koboshi/cache/WixResilientCachesTest.scala#L37).<br>
There is no current example for allowing writes anywhere else, if you absolutely need this contact us.<br>
Note - even fw test for writing .devEnv file was removed.

## External network access
`com.wix.e2e.http.exceptions.ConnectionRefusedException: Unable to connect to port 9901` or any other ConnectionException when trying to access external network addresses<br>
Such as accessing the nonLoopbackAddress or using http testkit’s checkExternal()<br>

- Solution 1: Fix your tests! There are very few cases where testing against external address is absoultely neccesary, and doing so makes your tests less hermetic, flakier and more environemnt/luck related.<br>
Examples of appropriate use cases:
  -  fw code has dedicated test for verifying that accessing the bootstrap server via external port is NOT allowed.
  -  An absoulte need for testing against live production 3rd parties - for exmaple firebase. In this case, you might conisder moving this test to not even be in the build but rather some peridoic automation job, for example.

- Solution 2: A special case of this is embedded processes downloaders. The mysql and mongo installers are the only ones that we've packaged in a way that will download them before the run (i.e 'repository rules').<br>
This type of error is the usual symptom - `Caused by: java.net.UnknownHostException: fastdl.mongodb.org` <br>
For other embedded processes you may need, for example **redis, ElasticSearch** etc, you should move to use the [dockerzied test kits](https://github.com/wix-private/bazel-tooling/blob/master/migrator/docs/docker.md).

- Solution 3: IF you have a good reason to do this! - use 'block-network = False’. See [example](https://github.com/wix-platform/wix-framework/blob/master/bazel_migration/internal_targets.overrides#L139).
## Host name resolving
`InetAddress.getLocalHost` (when run inside docker, as bazel does on jenkins+rbe) results in an external address (something like `9cc27d4c744e/172.17.0.9:0
`) → doing socket.bind on this causes `java.net.BindException: Cannot assign requested address (Bind failed)`


**IMPORTANT NOTE** - We are not currently sure if this fails in the same manner on the RBE step.
We are also currently investigating the option of fixing this problem by runing the docker agents with options similar to `--net=host`.<br>
If you have a type(2 or 3) case, please contact us at the migration slack channel (#bazel-migrate-support) and let us know - we want to see the failures and also see if the rbe step also failed this test.

Solutions:
1. If you are using something like `selectRandomPort`:
* Unless you really need to know the port of the server in advance, just do `Server(0)` - this will select a free random port.<br>
If you can provide the server with the ‘0’ port and then later ask again for the port it’s the best approach
* The only known example where supplying the '0'port is not acceptable is in fw’s test that start a bootstrap-jar in a separate process.
* If you DO need the port in advance, use the [PortRandomizer](https://github.com/wix-platform/wix-framework/blob/master/test-infrastructures-modules/io-test-kit/src/main/scala/com/wix/e2e/PortRandomizer.scala) from fw’s io-test-kit (which was [fixed](https://github.com/wix-platform/wix-framework/pull/1746/) to support this)

2. If you are doing something else related to networking, consider using 127.0.0.1 or “localhost” instead of InetAddress.getLocalHost

3. jmx/rmi connection errors -  fix by [setting rmi host to listen on localhost](https://github.com/wix-platform/wix-framework/pull/1775) (else defaults to external address when ran inside docker)

## OOM Jenkins errors
- Error 1: 
  - `Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x00000007a3600000, 169869312, 0) failed; error='Cannot allocate memory' (errno=12)
11:34:03 #
11:34:03 # There is insufficient memory for the Java Runtime Environment to continue.`

  - Possible Solution: This error will usually show up for a specific test. [Set the correct test `sizes`](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes-tests).

- Error2: 
  - `failed: Worker process quit or closed its stdin stream when we tried to send a WorkRequest:<br>
---8<---8<--- Exception details ---8<---8<---`

  - This is a known issue that sometimes happens when bazel runs in docker (which is how it runs on jenkins as well on RBE).<br>
Sandboxed builds have a tendency to encounter this more, due to their parallel work which maximizies resource utilization.
This means your build on the sandbox step may be flaky due to no fault on your side.<br><br>
Also note, that wile RBE build happens on docker too, this OOM issue won't ocure there, since each action is run in it's own searate container, as opposed to bazel doing a whole run inside one given container.
(This behaviour may also occur on local development in docker-for-mac, will be investigated in the future if necessary).

  - We are currently investigating 
    - how often this happens
    - whether this relates to insufficient resources in the jenkins agent’s docker host
    - whether this realtes in general to the inherent problems of running JVMs inside containers (and to how bazel itself behaves under this).

- Full error examples:
  - `Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x00000007a3600000, 169869312, 0) failed; error='Cannot allocate memory' (errno=12)
11:34:03 #
11:34:03 # There is insufficient memory for the Java Runtime Environment to continue.
11:34:03 # Native memory allocation (mmap) failed to map 169869312 bytes for committing reserved memory.
11:34:03 # An error report file with more information is saved as:
11:34:03 # /dev/shm/bazel-sandbox.630c07e5cba4628cd6ef58782828ddec/linux-sandbox/220/execroot/photography/bazel-out/k8-fastbuild/bin/albums-server/albums-webapp/src/it/scala/com/wixpress/exposure/albums/e2e/rpc/premium/premium.runfiles/photography/hs_err_pid15.log`
  - `failed: Worker process quit or closed its stdin stream when we tried to send a WorkRequest:
---8<---8<--- Exception details ---8<---8<---
java.io.IOException: Stream closed
	at java.lang.ProcessBuilder$NullOutputStream.write(ProcessBuilder.java:433)
	at java.io.OutputStream.write(OutputStream.java:116) ...
	at com.google.protobuf.CodedOutputStream$OutputStreamEncoder.write(CodedOutputStream.java:2928)...
	at com.google.devtools.build.lib.analysis.actions.SpawnAction.internalExecute(SpawnAction.java:287) ...
	at com.google.devtools.build.lib.skyframe.SkyframeActionExecutor.prepareScheduleExecuteAndCompleteAction(SkyframeActionExecutor.java:891)...
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)...
	at java.lang.Thread.run(Thread.java:748)
---8<---8<--- End of exception details ---8<---8<---
---8<---8<--- Start of log, file at /home/builduser/.cache/bazel/_bazel_builduser/7fc571e3089850336675617290d0b47c/bazel-workers/worker-5-Scalac.log ---8<---8<---`
