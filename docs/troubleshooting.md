# Troubleshooting Guide

- [Bazel Build Failures](#bazel-build-failures)
    - [Background and Definitions](#background-and-definitions)
    - [Possible Analysis Failures](#possible-analysis-failures)
    - [Possible Test Failures](#possible-test-failures)
    - [Additional Issues](#additional-issues)
- [Compare Failures](#compare-failures)
- [Migration Failures](#migration-failures)
    - [Maven Analysis Failures](#maven-analysis-failures)
    - [Transformation Fialures](#transformation-failures)
    - [Resolution Failures](#resolution-failures)
- [Sandbox Failures](#sandbox-failures)

## Bazel Build Failures

So you succeeded in migrating your project to Bazel **but** failed to build or test it using Bazel? 

Here, we explain how to solve Bazel build issues in 2 ways:

- **Manual** - Apply fixes on your migration branch (`bazel-mig-*`). See ([How to Run Bazel Locally](how-to-run-bazel-locally.md)).
- **Automatic** - Make sure that migrator generates BUILD.bazel files that fix the problem automatically **during the next migrations** by providing hints and overrides.

We recommend first checking the manual solution locally. You can also push the fix and re-run Jenkins. If that works well, apply the automatic solution.


### Background and definitions

#### Production Target
  
  A `scala_library` or `java_library`.
  
  Exodus writes a production target for each package found under `src/main`

#### Test Target

  A target that runs tests like `scala_specs2_junit_test`.

  Exodus writes a test target for each package found under `src/test` or `src/it`.

#### Test Support Target

  A `scala_library` with attribute `testonly = 1`.

  Exodus writes test support targets for each package which is under `src/test`, `src/it` or `src/e2e` but does not have any file that matches test pattern: prefix/suffix with `Test`,`IT` or `E2E`.

#### Filegroup Target

  A `filegroup` which other targets can refer to and depend on the files, very useful for cycles (see next section).

#### I can't find a production/test/test support target in my package/directory
  In cases where some packages have a cyclic dependency between them, Exodus writes the production,test, or test support target in their shared ancestor (e.g. if `com/something/ecom/stores` and `com/something/ecom/cart` packages have a cycle then the shared target will be in a BUILD.bazel file under `com/something/ecom`).




###  Possible analysis failures

#### (1) Production code that depends on test code

##### Problem

Sometimes there is a production target that depends on a test target (happens usually in testkits).
Exodus codes that dependency, but Bazel fails the build with the following message:

```
ERROR: (...) in scala_library rule <production-target>: non-test target '<production-target>' depends on testonly target '<test-target>' and doesn't have testonly attribute set
```

##### Explanation

Bazel test  or test support targets can depend on a production target **but not the other way around**.

##### Solution

**Manual**: Tell Bazel that the **production target** is actually a **test support target** by simply adding the `testOnly = 1` attribute.

1. Locate the target in your workspace (usually `scala_library`).
2. Add the attribute `testOnly = 1`.

**Automatic**: Let Exodus know that this target is [testOnly](overrides.md#optional-attributes).

#### (2) Missing symbols

##### Problem

Compilation fails on missing symbols with one of the following errors:

- `object __ is not a member of package __`
- `error: not found: value __`

##### Explanation

The `deps` list of that target is incomplete.

Exodus writes internal dependencies according to Codota (code index). If Codota's index is outdated or missing something, Exodus's result will also be incomplete.

[You can also manually check what is indexed in Codota](#how-to-verify-what-was-indexed-as-dependencies-of-a-file).

Codota has no knowledge of a dependency if it has no representation in the bytecode (e.g. constants or parameters of annotations) and it isn't represented in the imports (e.g. you're using `._`/`.*` imports or no imports since it's the same package).

If you have Scala code which depends on other Scala code **generated** from proto and you depend in Maven on `<classifier>proto</classifier>` this will also happen. 

##### Solution
**Manual**: Locate the target of the missing symbol and add its label to `deps` of the failing target.

**Note**: This will only solve the issue for your current branch. You still need to fix this in a way that Exodus will know of this dependency as well for future migration branches. See "Automatic" below.

**Automatic**: Read this [why Codota failed to index](#troubleshooting-bad-codota-index).

**Proto related**: Remove `<classifier>proto</classifier>` and `<type>zip</type>` from the dependency, push, wait for TC to build, and then re-run the migration.

#### (3) Missing additional generators for proto files

##### Problem

Compilation fails on missing scala classes that are auto-generated by custom proto generators (e.g. `call_scope`):

```
object MediaManagerGatewayWithCallScope is not a member of package com.wixpress.media.gateway.api
```

##### Solution
Add the following [internal target override](overrides.md#internal-target-overrides) for the relevant proto target label:

`"additionalProtoAttributes": "additional_generators = { \"call_scope\": [], },"`

#### (4) Maven dependency with RELEASE or LATEST version

##### Problem

When pom files contain dependencies with dynamic versions such as RELEASE or LATEST:

For example:

```
<dependency>
  <groupId>com.logentries</groupId>
  <artifactId>logentries-appender</artifactId>
  <version>RELEASE</version>
</dependency>
```
##### Explanation

The repository rule that downloads this dependency's jar fails to find this version.

Additionally, having a floating version is an anti-pattern in Bazel, which requires deterministic, reproducible builds.

##### Solution

- If the dependency is part of your repo, change it to the relevant explicit version:

    For example, find the version here:

    ```
    <dependency>
      <groupId>com.logentries</groupId>
      <artifactId>logentries-appender</artifactId>
      <version>3.2.1</version>
    </dependency>
    ```
- If the dependency is transitive, define the dependency directly in your pom with an explicit version. Also exclude the artifact in the direct dependency in your pom file.

    For example, if ```annotations-java5``` is fetched transitvely by ```com.wixpress.common:foo```, in addition to adding an explicit dependency on ```annotations-java5``` (like in case 1.), you also need to add an exclusion:

    ```
    <dependency>
      <groupId>com.wixpress.common</groupId>
      <artifactId>foo</artifactId>
      <exclusions>
        <exclusion>
          <groupId>org.jetbrains</groupId>
          <artifactId>annotations-java5</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
    ```


### Possible Test Failures

#### (1) Test target with no tests

##### Problem

Test target fails with the following message in th test log: `Was not able to discover any classes for archive`. 

For example:

```
==================== Test output for (some-target):
/dev/shm/bazel-sandbox.e0d6a9590dd3180f8e40342aabc7c552/5993067211771649541/execroot/other/bazel-out/local-fastbuild/bin/billing-onboarding-service/src/test/scala/com/wixpress/billing/onboarding/onboarding.runfiles/other/billing-onboarding-service/src/test/scala/com/wixpress/billing/onboarding/onboarding: line 257: execpath: command not found
JUnit4 Test Runner
.E
Time: ...
There was 1 failure:
1) initializationError(io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite)
java.lang.IllegalStateException: <b>Was not able to discover any classes for archive</b>=[Ljava.lang.String;@397fbdb, prefixes=Set(Test), suffixes=Set(Test)
```

Another example:
```
1) initializationError(io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite)
java.lang.IllegalStateException: Was not able to discover any classes for archive=[Ljava.lang.String;@130d63be, prefixes=Set(IT, E2E), suffixes=Set(IT, E2E)
```

##### Explanation 1
There is a file in the package with a name that matches pattern `Test*` or `*Test` or `IT*` or `*IT` but inside that file there is no actual test class to run. This often happens with test support code like `TestAspectSupport.scala`.

Based on the filename, Exodus incorrectly writes a target that is supposed to run a test.

Other than such files, no other files containing tests exist. So Bazel errors out. 

If you have a mix of test support and tests, it works.

##### Solution

**Manual**: Tell Bazel that the target is actually just a test support target:

1. [Locate](#how-to-locate-the-definition-of-failing-test-target) the problematic BUILD.bazel file and the target in your workspace.
2. Add the attribute `testOnly = 1`.

    Example:

    ```
    # BUILD.bazel file
    scala_library(
      name = "some_target",
      srcs = glob(["*.scala"]),
      )
    ```
  
    Turns into:
  
    ```
    # BUILD.bazel file
    scala_library(
      name = "some_target",
      testOnly = 1,
      srcs = glob(["*.scala"]),
      )
    ```
**Automatic**: Use an [override](overrides.md#internal-target-overrides) to change the `testType` to `None`.

##### Explanation #2

If a test class is declared with `Specs2` specification instead of `SpecWithJUnit` (or other equivalent situations).

##### Solution

Update the test class to have JUnit support.

#### (2) Tests that use EmbeddedMySql/EmbeddedMongo

##### Problem

**EXAMPLES to outputs:**
NOTE: We are still collecting examples to test failures but this section is relevant to any test target that uses MySQL.

```
com.something.framework.test.env.ITEnvManagedServiceException: Failed to start managed testing service [ITEmbeddedMysql]
    at
    ...
    at com.wixpress.framework.test.env.AsyncCompositeManagedService.$anonfun$fanout$2(CompositeManagedService.scala:24)
<b>Caused by: java.lang.IllegalArgumentException: Could NOT create Directory /home/builduser/.embedmysql</b>
    at de.flapdoodle.embed.process.store.LocalArtifactStore.createOrCheckDir(LocalArtifactStore.java:62)
    ...
    at de.flapdoodle.embed.process.store.ExtractedArtifactStore.checkDistribution(ExtractedArtifactStore.java:60)
    at de.flapdoodle.embed.process.runtime.Starter.prepare(Starter.java:56)
    at de.flapdoodle.embed.process.runtime.Starter.prepare(Starter.java:49)
    at com.wix.mysql.EmbeddedMysql.<init>(EmbeddedMysql.java:41)
    at com.wix.mysql.EmbeddedMysql$Builder.start(EmbeddedMysql.java:169)
    at com.wixpress.mysql.ITEmbeddedMysql.doStart(ITEmbeddedMysql.scala:37)
    at com.wixpress.framework.test.env.InitGuardedManagedService.tryToStart(AbstractManagedService.scala:34)
```

##### Explanation

For sandboxed test targets that use embedded MySQL or Mongo, [specify additional attributes](#checklist-for-test-target-that-uses-embeddedmysqlembeddedmongo).

#### (3) Tests are failing with timeout

##### Problem

Test fails with output similar to:

```
//server/wix-campaigns-advertiser/wix-campaigns-advertiser-web/src/test/scala/com/wix/advertiser/ops:ops TIMEOUT in 61.0s
```

##### Solution
Make sure the timeout is indeed too low since using large timeouts will obviously slow down the build.  

Override manually - Add the "size" attribute as described [here](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes-tests).

Override for Migration - Add the "testSize" attribute as described [here](overrides.md#optional-attributes).

#### (4) Tests are failing to read files from resources

##### Problem

Test fails with output similar to:
    
```
java.io.FileNotFoundException, with message:
file:/home/builduser/.cache/bazel/.../main/resources/libresources.jar!/emails_vmroot/email-template-it.vm (No such file or directory)"
```
   
The test or production code is trying to read a classpath resource under `src/main/resources` (or `src/test/resource`, etc.) as a file.

This works in Maven where the resources are files, but in Bazel the resources are packaged in a jar. They can be loaded using the classloader, but not as regular files.

##### Solution

If the classpath resource is only loaded into test code, just load it from classpath. You can use Guavas `Resources` utility.

If your production code expects to read the resources as files, you will need to extract them to a temporary directory.

You can use the `ClasspathResource` object from `io-test-kit` module from the framework.

Add the following to your POM.xml:

```xml
<dependency>
    <groupId>com.wixpress.framework</groupId>
    <artifactId>io-test-kit</artifactId>
    <scope>test</scope>
</dependency>
```

Then, in your test environment setup code:

```scala
import com.wix.e2e.ClasspathResource
// for multiple resources
val resourceRoot = ClasspathResource.convertToFiles(
  "emails_vmroot/assets/locale/emails_en.json", 
  "emails_vmroot/email-template-it.vm"
  )
// for single resource
val pathToResourceFile = ClasspathResource.convertToFile("emails_vmroot/assets/locale/emails_en.json")
```

Pass the `resourceRoot` or `pathToResourceFile` to your production code. For example, via a config file (ERB).

#### (5) Tests are failing to read log files

##### Problem 
Tests are failing to read logs due to Logback writing logs to `target/logs`.

The server logger is configured via `logback.xml` or `logback-test.xml`.

While some projects configure their `logback-test.xml` to simply write to `STDOUT`, some configure this XML to write to a file in the file system (usually under the `target/test-out` directory).

Bazel runs the tests in isolated environment and `./target/test-out` may not be available.

##### Solution

Change `logback-test.xml` to write to `${TEST_UNDECLARED_OUTPUTS_DIR}/some-file.log`. This directory gets archived after the test execution ends and then you can view the log in the zip file under: `./bazel-out/darwin-fastbuild/testlogs/<package-name>/<target-name>/test.outputs/outputs.zip`.

To make sure you are still compatible with Maven you can write `${TEST_UNDECLARED_OUTPUTS_DIR-./target/test-out}/some-file.log` and then logback would use Bazel's `${TEST_UNDECLARED_OUTPUTS_DIR}`, and if not found , will use `target/test-out`.

For example:

```xml
<appender name="GENERAL_FILE_LOG" class="ch.qos.logback.core.FileAppender">
    <append>false</append>
    <file>${TEST_UNDECLARED_OUTPUTS_DIR-./target/test-out}/app-store-wix.general.log</file>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} %-5level class=[%logger{0}] %marker %msg%n</pattern>
    </encoder>
</appender>
```

If you happen to have a line that tries to load a file from the Maven structure path, like `<include file="src/main/resources/logback-overrides.xml"/>`, you must replace it with `<include resource="logback-overrides.xml"/>` so it will be loaded from classpath, as Maven the directory structure will not look the same on Bazel tests runtime).

#### (6) Tests are failing because koboshi cannot write to cache directory

##### Problem
Test fails with output similar to the below:

```scala
[com.wix.hoopoe.koboshi.cache.ReadOnlyTimestampedLocalCache]: Factory method 'petriResilientCache' threw exception; nested exception is java.lang.IllegalStateException: 
***************************************************************
cache folder /tmp/_tmp/4a2ad955f657ae1ad7600fe72f00d923/cache/wix/xxxx is a non writable directory
This may be because you are running in the dev environment but have not signalled it to be so.
Try "touch /home/builduser/.devEnv" to solve it.
See EnvironmentProvider for additional details
***************************************************************
```
  
This tends to happen when you have an `IT` test that starts more then 1 bootstrap server.
Because Bazel is faster than Maven, both servers attempt to write into the same koboshi folder and lock each other out.

Another possibility is that this happens because you are running a bootstrap server in your own separate process. If this is the case, you need to pass in the `-Dwix.enviroment=CI` jvm argument. 

##### Solution

Add a fakeManifest with specific artifact-ids:

```
BootstrapManagedService(OneOfYourBootstrapServers, Options(
        port = Some(oneOfYourBootstrapServersPort),
        manifest = Some(FakeManifest.sample.copy(ImplementationArtifactId = "OneOfYourBootstrapServersArtifactId").toPath)
      ))
```

This affects the cache folder path and prevents the collision.

#### (7) Tests are failing with insufficient memory 

##### Problem

Test fails with output similar to:

```scala
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 110624768 bytes for committing reserved memory.
# An error report file with more information is saved as: ......
```

##### Solution
Override manually - add the "size" attribute as described [here](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes-tests).

Override for migration - add the "testSize" attribute as described [here](overrides.md#optional-attributes)

#### (8) Tests are failing with java.net.UnknownHostException when com.mongodb.MongoClient is trying to connect to mongo, which is running as a Docker container

##### Problem
In Bazel's network configuration, every Docker container exposes its hostname as its original name. 

For example, if the container was named `mongodb-8T5CUgtL6trgfmfM0BDB`, its hostname will be the same. `MongoClient`, when given the hostname to connect to, does `hostname.toLowerCase()` under the hood. So if your container name contains at least one upper case, `MongoClient` will fail to connect to it.  

##### Solution
Make sure your Docker container is named using only lower case letters.

#### (9) Tests are failing with java.lang.ClassFormatError: Absent Code attribute in method that is not native or abstract in class file

##### Problem
A macro code is built with the `scala_library` rule.

##### Solution
Change the code to be built via the `scala_macro_library` rule.

**Manual**: Change the `scala_library` call to `scala_macro_library` in the relevant BUILD.bazel file.

**Automatic**: Add the below to post_migraiton.sh:

```
buildozer 'set kind scala_macro_library' <target>
```

### Additional issues

- Tests need ~~`mysql`~~ or `mongo`
- Tests that run docker
- IT that was wrongfully tagged as UT - port in use, parallelization, cannot open socket etc...
- tmp directory is too long
- Missing symbol
   - Was supposed to get there from third party but was filtered out
   - Codota is not refreshed with latest code
   - wildcard import with custom types
- test is too small
- Unidentified tests

### How to locate the definition of target that fails to compile:

1. Locate the first location of `ERROR:` just before the failure in the log:
    <pre><code>ERROR: <b>/full/path/to/build/BUILD.bazel</b> :12:1: scala //foo/bar:<b>target_name</b> failed (Exit 1)</code></pre>
2. The path to BUILD.bazel file is given.
3. Look inside for the rule according to the label.

### How to locate the definition of failing test target

1. Locate the first location of `==================== Test output for **(some-label)**:`` **above** the failure message
    <pre><code>
    ==================== Test output for <b>//some/package:some-target</b>:
    12:06:55 JUnit4 Test Runner
    12:06:55 .E
    12:06:55 Time: 0.021
    12:06:55 There was 1 failure:
    12:06:55 1) initializationError(io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite)
    12:06:55 java.lang.IllegalStateException: Was not able to discover any classes for archive=[Ljava.lang.String;@179ece50, prefixes=Set(IT, E2E), suffixes=Set(IT, E2E)
    12:06:55 at io.bazel.rulesscala.test_discovery.PrefixSuffixTestDiscoveringSuite$.discoverClasses(DiscoveredTestSuite.scala:51)
    12:06:55 at io.bazel.rulesscala.specs2.Specs2PrefixSuffixTestDiscoveringSuite.<init>(Specs2RunnerBuilder.scala:25)
    12:06:55 at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
    12:06:55 at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
    </code></pre>
2. From the label you can understand the location of the BUILD.bazel file and the name of the target:
    For label //some/foo/bar:baz
    - The location of the BUILD.bazel file is <span><code>(repo-root)/<b>some/foo/bar</b>/BUILD.bazel</code></span>
    - The name of the target is `"baz"`

### How to verify what was indexed as dependencies of a file
1. In Jenkins - open **02-Debugging** tab
2. Go to **Deps of Specific File by 
**
3. Click "Build with Parameters"
4. Supply parameters:
   * **module_relative_path** - relative path to **maven** module (where the file located) from repository root
   * **use_tests** - mark if the file is under `src/test` or `src/it` folders and not under `src/main`
   * **relative_file_path** - the relative file path (for instance `com/wixpress/bootstrap/jetty/JettyBasedServer.scala`)
   * **skip_classpath** - check only if you want to see the results of the last migration run. If you made changes and want to see the new results by codota - please uncheck it
5. Look under the logs for results
6. Missing some deps?

### Troubleshooting bad codota index
1. Check that the module is green in JVM-TC
    - **If it's red - fix that module!**

2. Check that the module / the dependencies produce `sources` jar or `test-sources` jar
    - Go to artifactory: (for groupId:`some.group` artifactId:`artifactId` version:`1.0-SNAPSHOT`) [https://repo.dev.wixpress.com/artifactory/libs-snapshots/some/group/artifactId/1.0-SNAPSHOT/]()
    - See if you can find there `sources` jar or `test-sources` jar and check that the last updated timestamp makes sense.
    - If you can't find `sources` or `test-sources` jar check the following:
      - make sure the maven module inherits from `base-parent`:
        ```xml
        <parent>
          <groupId>com.wixpress.common</groupId>
          <artifactId>wix-scala-parent-ng</artifactId>
          <version>100.0.0-SNAPSHOT</version>
          <relativePath/>
        </parent>
        ```
        OR
        ```xml
        <parent>
          <groupId>com.wixpress.common</groupId>
          <artifactId>wix-java-parent-ng</artifactId>
          <version>100.0.0-SNAPSHOT</version>
          <relativePath>../wix-java-parent-ng</relativePath>
        </parent>
        ```
        OR
        ```xml
        <parent>
          <groupId>com.wixpress.common</groupId>
          <artifactId>wix-base-parent-ng</artifactId>
          <version>100.0.0-SNAPSHOT</version>
          <relativePath>../wix-base-parent-ng</relativePath>
        </parent>
        ```
      - make sure you don't have custom configuration to `maven-sources-plugin` or `maven-jar-plugin` (NEED TO ADD CODE EXAMPLE?)



3. Dependencies relation can be **sometimes** missed when either:
  - using wildcard import
  - when using symbols of the same package without explicitly importing them.
    - example of such a case is spring xml files.

    **If you have such case - please report to us (so we can generelize it and solve it in the future), but for quick fix**
  - add explicit imports to the missing symols
  - After green CI, wait ~5 more minutes so Codota would index the changes
  - (if you want to make sure - use the debugging job to verify that the deps of that module was changed)

### Checklist for test target that uses EmbeddedMySql/EmbeddedMongo:
First, you must use ITEmbeddedMysql (and not wix-embedded-mysql directly) / must use ITEmbeddedMongo (and not the deprecated EmbeddedMongo).
See fw’s [mysql readme](https://github.com/wix-platform/wix-framework/tree/master/test-infrastructures-modules/mysql-testkit) or [mongo readme](https://github.com/wix-platform/wix-framework/tree/master/test-infrastructures-modules/mongo-test-kit).
<br>Test targets that use EmbeddedMySQL/Mongo must be declared in [**overrides mechanism**](overrides.md#possible-attirbutes) with the following:
- [ ] Test target must set `additionalDataDeps`:
  - `@mysql_default_version//:binary`- [example](https://github.com/wix-private/promote/blob/master/bazel_migration/internal_targets.overrides#L99)
  - `@mongo_default_version//:binary`- [example](https://github.com/wix-private/users/blob/67b5abea1aeedca3e05d04b615cf9e534928d75c/bazel_migration/internal_targets.overrides#L261)
  - Note - you can also edit this in place in the actual BUILD file like [mysql example](https://github.com/wix-platform/wix-framework/blob/8e98b032762d9e9cbe19c587adeb4bf1a605792d/test-infrastructures-modules/mysql-testkit/src/it/scala/com/wixpress/mysql/BUILD.bazel#L11) or [mongo example](https://github.com/wix-platform/wix-framework/blob/8e98b032762d9e9cbe19c587adeb4bf1a605792d/mongo-modules/wix-mongo-config/src/it/scala/com/wixpress/mongo/BUILD.bazel#L10) if you wish to run a build on a brnach without running migration - make sure you add only the deafult tag OR a specific verison, no need for both. See here on how to [run this for your specifc target](troubleshooting-sandbox-failures.md#i-want-to-run-only-1-step-with-sandboxing).

**But wait, I need a custom mysql version, what do I do?** Test targets that use a **custom** mysql version need to declare:
- [ ] Use the correct [ITEmbeddedMysql c'tor](https://github.com/wix-platform/wix-framework/blob/9cf60b8468d9a0d6c2389d3c6ff7bbe882efe567/test-infrastructures-modules/mysql-testkit/src/it/scala/com/wixpress/mysql/EmbeddedMysqlIT.scala#L55)
- [ ] Test target must set `additionalJvmFlags` with an additional flag:
  - `-Dexample.custom.mysql.version=$(location @mysql_5.6_latest//:binary)` (replace 5.6_latest with your version)
- [ ] Test target must set `additionalDataDeps`:
  - `mysql_5.6_latest//:binary` (replace 5.6_latest with your version)
- Example [test target](https://github.com/wix-platform/wix-framework/blob/3a27ec707becf291fddb073b0bc62720e9f00d00/bazel_migration/internal_targets.overrides#L87) (note that this sets 2 versions of mysql, you will most probably need ONLY ONE).

**But wait, I need a custom mongo version, what do I do?** Test targets that use a **custom** mongo version need to declare:
- [ ] Use the correct [ITEmbeddedMongo c'tor](https://github.com/wix-platform/wix-framework/blob/4d97fc0f22c65140ca9d748ac9412be2c4a2db52/test-infrastructures-modules/mongo-test-kit/src/it/scala/com/wixpress/framework/test/embeddedMongo/EmbeddedMongoWithDownloadConfigIT.scala#L24)
- [ ] Test target must set `additionalJvmFlags` with an additional flag:
  - `-Dexample.custom.mongo.version=$(location @mongo_3.3.1//:binary)` (replace 3.3.1 with your version)
- [ ] Test target must set `additionalDataDeps`:
  - `@mongo_3.3.1//:binary` (replace 3.3.1 with your version)
- Example [test target](https://github.com/wix-platform/wix-framework/blob/4d97fc0f22c65140ca9d748ac9412be2c4a2db52/bazel_migration/internal_targets.overrides#L15) (note that this sets 2 versions of mongo, you will most probably need ONLY ONE).


**Why is this good for you** / why you should fix this even if it's only failing in your sandbox step:
* If your ITs are passing in remote execution then usually the sandbox failure just means you're not downloading your dependencies up front. This means, for example, every test run will download 350MB of MySQL installer since you didn’t declare it properly --> slower build.<br>
In the near future RBE beahviour will be fixed so that it has full network sandboxing, and then your tests will start failing there too.
* Additionaly, once we all start developing locally with the fully sandboxed and parallelized `bazel test //...`, these tests will fail lcoally for you too.<br>
The only theortical way to workaround this would have been to make `block_network = false` by default, which would cancel out all the hermeticity beauty we gain from bazel.


**Additionally** - if the migrator thinks the test is Unit Test (it was named with `Test` prefix/suffix) - it is recommended to change it to IT or E2E (name wise and probably also location wise just for clarity). If you really insist on leaving the test as Unit Test - you should at least use the [**overrides mechanism**](overrides.md#possible-attirbutes) to set the `tags` to `ITE2E`.

**Example to overrides needed for target under unit test the needs EmbeddedMySQL:**:

  ```json
      {
        "label": "//foo/bar/src/it/scala/com/example:mysql",
        "additionalDataDeps": [
          "@mysql_default_version//:binary",
        ],
        "tags": "ITE2E"
      }
  ```

### Running bazel build on results in `File name too long`:

1. For some projects running `bazel build //...` may result in `File name too long`. If you open the offending BUILD.bazel 
file you can see name for the library (`agg=...`) is really long. This means that there is a big cycle between some of 
the packages in your build.  
2. You can use the `newName` in [**overrides mechanism**](overrides.md#possible-attirbutes) to replace the sythetic name with a shorter one, or you can try to remove the cycle from your code or make it smaller.    
NOTE: If you use the override you're likely to experience performance problems in Bazel once we move to it since the target is very coarse grain (less parallelism and cache hits)


### Locally running `bazel build` or `bazel test` results in

```
Worker process quit or closed its stdin stream when we tried to send a WorkRequest:
...
...
...
at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
at java.lang.Thread.run(Thread.java:748)
```
Run `bazel version` and make sure that you are running the latest Bazel stable release (at the time of writing this `0.10.1`). 


## Compare Failures

Exodus collects all the tests that Maven ran and compares them against the tests that ran in Bazel.

If Maven failed to run, Exodus cannot perform comparison.

### Issue reading assembly descriptor

If you get something like this in the build log:

```
 Reading assembly descriptor: maven/assembly/tar.gz.xml
 ...
  Failed to execute goal org.apache.maven.plugins:maven-assembly-plugin:2.2.1:single (default) on project cashier-merchant-settings: Failed to create assembly: Error creating assembly archive wix-angular: You must set at least one file.
```

A common reason for Maven failure is npm modules that have pom.

In regular CI, we run `npm build`, and then `mvn install` only to create package using `maven-assembly-plugin`. This run would fail if `npm build` did not run just before it.
In the migration server, we simple don't run `npm build` and that's why maven would fail on missing descriptor file.

The solution is to add `shouldSkipAssembly` property to the pom with the default value of `false`.

On the migration server we run with override property `-DshouldSkipAssembly=true`.

It should look like this:

```xml
    <properties>
        <shouldSkipAssembly>false</shouldSkipAssembly>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>2.2.1</version>
                <configuration>
                    <descriptors>
                        <descriptor>maven/assembly/tar.gz.xml</descriptor>
                    </descriptors>
                    <skipAssembly>${shouldSkipAssembly}</skipAssembly>
                    <appendAssemblyId>false</appendAssemblyId>
                    <finalName>${project.artifactId}-${project.version}</finalName>
                </configuration>
                <executions>
                    <execution>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
``` 

### Issue reading different test number for Maven and Bazel

If the number of total tests in Bazel and Maven is different, check if you use `Fragments.foreach` or any other kind of loops to generate the names of test cases in the files that have differences.

Maven does not generate names for tests with `Fragments` correctly. It will produce duplicated test names in the report. The duplications are ignored by the compare process, and that's why you may get a lower number of Maven tests compared to Bazel. 

A way to fix this would be to either:

- Do not use `Fragments`. If possible, rewrite the test names to use static names instead
- After verifying tests are passing in Bazel, mute the problematic test suites by adding them to `muted_for_maven_comparison.overrides` file in your `bazel_migration` directory.

## Migration Failures

The following relates to failures in the `migrate` phase. As a reminder, roughly speaking, the `migrate` phase is concerned with being able to **structurally** understand your repository so it can **successfully** output the Bazel build descriptors (`WORKSPACE` file and `BUILD.bazel` files).

### Maven Analysis failures

-	Good news is that we haven't found any failures so far.
-	Bad news is that it probably just means we haven't looked at enough crazy maven setups :) If `migrate` fails here, please tell us about it.

### Transformation failures

####	Code Analysis

**Note:** In the following section file paths are relative to a source dir (`src/it/scala`) as Codota only gets our jars but not access to out VCS. If there is a path then somewhere in the failure stack there is also the `SourceModule` which denotes the source maven module we're looking the file in.

-	**Composite**- Denotes an aggregator of analysis failures.
-	**AugmentedFailure**- Denotes a failure which is augmented by some additional metadata along the calling stack. See the inner most failure to understand what the root issue. See *Failure Metadata* section below for more detail on what each augmented metadata means.
-	**SourceMissing**-
    -	What: Happens when Codota reports a file exists but we can't find it. It might mean the file was renamed, moved or deleted and for some reason it is not reflected in Codota.
    -	Arguments: `filePath: String, module: SourceModule`
    -	What do to:
        -	Verify you're on `master` and haven't moved the file locally.
        -	Verify the file is under `src/main`/`src/it`/`src/test`/`src/e2e` and under these it's either `java`/`scala` (e.g. `src/e2e/scala` is supported, `src/foo/java` and `src/main/kotlin` aren't). If you have a different need, talk to us.
        -	Verify Codota received the latest jar (see below `Verifying Codota received your artifact`).
        - Note: If you have Java/Scala files under resources folder (for example for tests), either delete them or mute them in a [source override](overrides.md).
-	**MultipleSourceModules**\-
    -	What: Happens when a file depends on the same fully qualified class which exists in several neighboring source modules in your own project. If the other modules are from different projects or one of the results is of the current module then does win by locality. Otherwise It's hard (and sometimes impossible) to know which version inside your repo you're using so we're failing here.
    -	Arguments: `modules: Iterable[SourceModule]`
    -	What to do: Search for `internalDepGroup` in the log to see the multiple files which define the same fully qualified class name. Align on one version by renaming, deleting or consolidating the multiple versions. Push your changes and wait for a few minutes after your build finishes successfully to run the migrate again.
-	**SomeException**
    -	We don't really know what happened :( If it's an instance of CodotaHttpException then check the status code since it might give more detail. If you can't make sense after a couple of minutes (and after reading the additional metadata), talk to us.
-	**MissingAnalysisInCodota**\-
    -	What: Happens when Codota knows about a `.java`/`.scala` source file which it couldn't analyze. We don't know exactly why this happens. You sould check the following cases which we've seen this happen in:
        -	**Misconfigured Test-Jars**- If you produce a `test-jar` and the `configuration` of the test-jar goal is global and not inside a specific `execution` then Codota will receive all of your test sources but only some of your test bytecode causing a mismatch. This will also happen if you have an explicit version of the `maven-jar-plugin` older than 3.0.2.
        -	**Sources without bytecode** (for example all code is commented out)- check the files mentioned in the error json, if this file has no code, Either delete them or mute them in a [source override](overrides.md). If you're deleting push your changes and wait for a few minutes after your build finishes successfully to run the migrate again.
        -	**Multiple package objects in same package.scala**- Move one of them to the correct package because according to the scala [spec](http://scala-lang.org/files/archive/spec/2.12/09-top-level-definitions.html#package-objects) package.scala should only contain one package object. Push your changes and wait for a few minutes after your build finishes successfully to run the migrate again.
        -	**Package object not in package.scala**- Your package object must be defined in package.scala. That is why you can't have multiple package objects in same package.
        -	**Mismatch of package/file location** (Codota employ many heuristics to solve this but they still miss (though rarely)- Move them to the correct place according to the package declaration. Push your changes and wait for a few minutes after your build finishes successfully in CI to run the migrate again.
        - **if you have maven-war-plugin** - If you have `maven-war-plugin` (e.g GAE artifact), set `attachClasses` attribue as true in plugin configuration.
          ```xml
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <configuration>
              <attachClasses>true</attachClasses>
            </configuration>
          </plugin>
          ```
        - **if you have maven-shade-plugin** - If you have `maven-shade-plugin` (e.g billing-scripts artifacts), set `shadedArtifactAttached` attribute as true in plugin configuration
          ```xml
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <configuration>
              <shadedArtifactAttached>true</shadedArtifactAttached>
              <shadedClassifierName>jar-with-dependencies</shadedClassifierName>
            </configuration>
          </plugin>
          ```
        -	**Jars not getting to codota**- Verify Codota received the latest jar (see below `Verifying Codota received your artifact`).
        - **class is part of really big jar** - if the class is part of an uber jar or jar that is bigger - Codota would avoid indexing it. Contact us and we will figure it out.
-	**Failure Metadata**
    -	SourceModuleFilePath(filePath: String, module: SourceModule)- Adds the source module and file path.
    -	MissingArtifactInCodota(artifactName: String)- Adds the artifactName which we failed to find in Codota.
    -	InternalDep(dep: Iterable[DependencyInfo.OptionalInternalDependency])- Adds the dependency-group when we have any problem with one of its instances. A DependencyGroup is bigger than one when you have multiple files containing the same fully qualified class name.
    -	InternalDepMissingExtended(depInfo: DependencyInfo)- Adds the specific DependencyInfo who has no Extended part which we use. We've never seen it happen but would rather fail with more context if it happens.

####	Graph analysis

-	Cycles - applies if you get an error message which starts with `cycle: Trying to combine` or contains `full subGraph containing cycle`
    -	cyclic dependency between your src/it and src/test- In maven src/it and src/test are the same unit so cyclic dependency between them is not a problem. In bazel it is since we don’t want BUILD.bazel files globbing files from entire source folders. To identify if you have a problem: https://github.com/wix-private/core-server-build-tools/tree/master/scripts/dependency-detector To fix: Usually it’s to move a few test helper classes from src/it to src/test and break the cycle this way
    -	cyclic dependency between your src/it & src/test and cyclic between src/something/java & src/something/scala- In maven src/it and src/test are the same unit so cyclic dependency between them is not a problem. In bazel it is since we don’t want BUILD.bazel files globbing files from entire source folders. Same for cycles between src/something/java and src/something/scala To identify if you have a problem: https://github.com/wix-private/core-server-build-tools/tree/master/scripts/sources-unifier To fix: Use the above tool
-	`can't find resourceKey...`- This probably means there's a bug somewhere. That's what we found the last two times this happened. Talk to us.

### Resolution failures


-	PropertyNotDefinedException- you're depending on a `pom` file which contains incorrect definitions. For example it contains a dependency with a version containing a property but nothing declares the property. You might not have a problem since you don't depend on that specific dependency but our resolver requires the `pom` to be valid. If it's your `pom` then please fix it by removing the faulty entry or adding a property with a default value (if you're not sure how, talk to us).

-	`packaging not supported for`- We currently support only `jar` and `pom` packaging types. If you have a different need please talk to us.


### Verifying Codota received your artifact
- Go to artifactory: (for groupId:some.group artifactId:artifactId version:1.0-SNAPSHOT) https://repo.dev.wixpress.com/artifactory/libs-snapshots/some/group/artifactId/1.0-SNAPSHOT/
- Find the latest sources jar, test-sources.jar, "regular" jar and codota-tests.jar and send all of these filenames to us alongside with your groupId and artifactId. We'll get in touch with codota and check if they received these and whether they were indexed or not.

### Proto dependencies

```xml
<!--example-->
<dependency>
   <groupId>com.wixpress.payment</groupId>
   <artifactId>payment-public-pay-api</artifactId>
   <version>1.91.0-SNAPSHOT</version>
   <classifier>proto</classifier>
   <type>zip</type>
 </dependency>
```

Migrator does skips proto dependencies for any targets under jvm maven modules
(== modules that have scala / java Code). If you have jvm module that depends on proto dpendency (`type`==`zip` && `classifier`==`proto`) - you probably have one of the cases:

1. **Module that does not have proto files** - you should convert the proto dependency to regular dependency: in the module `pom.xml`, locate the proto dependency and remove the the `type` and the `classifier`.

```xml
<!--example-->
<dependency>
	 <groupId>com.wixpress.payment</groupId>
	 <artifactId>payment-public-pay-api</artifactId>
	 <version>1.91.0-SNAPSHOT</version>
 </dependency>
```
2. **Module that has both jvm source files and proto files** - create another maven module just for you proto files, the `pom.xml` should have the proto dependencies and `grpc` plugin, the other module should depend on the new module.

## Sandbox Errors

### Known Issues
0. [OOM errors](#oom-jenkins-errors) on Jenkins `run-bazel-sandboxed'. 
1. Any error to do with embedded Mysql / Mongo, see [here](#checklist-for-test-target-that-uses-embeddedmysqlembeddedmongo).
2. Errors like `caused by FileSystem readonly` or `Could not create dir`. See [Filesystem write errors](#file-system-writes).
3. Errors like `com.wix.e2e.http.exceptions.ConnectionRefusedException: Unable to connect to port 9901` or `Caused by: java.net.UnknownHostException: fastdl.mongodb.org`, or any other ConnectionException. See [external network access](#external-network-access).
4. Erros like `java.net.BindException: Cannot assign requested address (Bind failed)`. See [hostname resolving](#host-name-resolving).

### What is Sandboxing?
One of the steps in the migration is run-bazel-sandboxed.

This step runs all of your tests as you would run in a local development as well.

That means running with `bazel test //...` instead of separating unit tests and ITs like today:

`bazel test --test_tag_filters=IT --strategy=TestRunner=standalone --jobs=1`

The ideal way to tell bazel to run all tests in your project is `bazel test //...`.

This parallelizes the execution and run each test in a hermetic (i.e isolated, repeatable..) sandbox.

As with all sandboxes, this affects two main aspects - filesystem and network interactions.

### Why can't I enjoy this awesomeness locally?

- The reason we disable the sandbox (`--strategy=TestRunner=standalone`):
Bazel cannot reach full network isolation on Mac (due to OSx limitation).

    This is a problem since, unfortunately, our IT and E2E tests use the same common ports (as opposed to randomized ports).

- The reason we run sequentially (`--jobs=1`) - is due to a a few hermeticity problems we're investigating currently (sockets, hostname resolving and docker).

We're actively working on being able to develop locally with the full benefits of `bazel test //...`, most probably based on using docker-for-mac.

### I want to run only 1 step with sandboxing

* Go to *"debugging"* tab in your migration folder 
* Find the job *"Test or build partial project"*
* Enter branch name (usually `bazel-mig-X`)
* Enter full label of target or wildcard (`//foo/bar/...`)
* Select if you want to run `bazel build` or `bazel test`

Notes:

* The job does not run migration - select a branch that has bazel in it
* Bazel would build anything needed for the targets you selected (even targets outside of the scope you selected)

### All my other steps are Green. Why do I have to make this sandbox step green too?

In the meantime, this `run-bazel-sandboxed` step in Jenkins is the best way to make sure your code is prepared for local development.

In addition, the actual CI builds on RBE also run in sandboxed mode.

RBE - Google "Remote Build Execution" - A flexible workers farm for extremely fast builds.

At the moment the network sandboxing there is not full, but that is expected to change in the near future and when this happens tests will fail there too in some cases.

### How can I see the log of a failed test?
- Navigate to a link like this: http://ci-jenkins-poc0a.42.something.net/job/something/job/run-bazel-sandboxed/16/artifact/.
Click the  link to “build artifacts” -> “bazel-out/k8-fastbuild/testlogs”

### File System Writes
Errors such as: `caused by FileSystem readonly`, `Could not create dir`.

Writing to non tmp destinations violates hermeticity.

FileSystem interactions should be limited to the default java tmp dir, by using Files.createTempDirectory() or such, like [here](https://github.com/wix-platform/wix-framework/blob/f86fea548916977c01038bd9119d9b15dcbb3d32/koboshi-modules/wix-koboshi/src/test/java/com/wixpress/framework/koboshi/cache/WixResilientCachesTest.scala#L37).

There is no current example for allowing writes anywhere else, if you absolutely need this contact us.

Note: even fw test for writing .devEnv file was removed.

### External network access
`com.wix.e2e.http.exceptions.ConnectionRefusedException: Unable to connect to port 9901` or any other ConnectionException when trying to access external network addresses.

Such as accessing the nonLoopbackAddress or using http testkit’s checkExternal().

- Solution 1: Fix your tests! There are very few cases where testing against external address is absoultely neccesary, and doing so makes your tests less hermetic, flakier and more environemnt/luck related.

Examples of appropriate use cases:
  -  fw code has dedicated test for verifying that accessing the bootstrap server via external port is NOT allowed.
  -  An absoulte need for testing against live production 3rd parties - for exmaple firebase. In this case, you might conisder moving this test to not even be in the build but rather some peridoic automation job, for example.

- Solution 2: A special case of this is embedded processes downloaders. The mysql and mongo installers are the only ones that we've packaged in a way that will download them before the run (i.e 'repository rules').

This type of error is the usual symptom - `Caused by: java.net.UnknownHostException: fastdl.mongodb.org`

For other embedded processes you may need, for example **redis, ElasticSearch** etc, you should move to use the [dockerzied test kits](docker.md).

- Solution 3: IF you have a good reason to do this! - use 'block-network = False’.

### Host name resolving
`InetAddress.getLocalHost` (when run inside docker, as bazel does on jenkins+rbe) results in an external address (something like `9cc27d4c744e/172.17.0.9:0
`) → doing socket.bind on this causes `java.net.BindException: Cannot assign requested address (Bind failed)`


**IMPORTANT NOTE** - We are not currently sure if this fails in the same manner on the RBE step.
We are also currently investigating the option of fixing this problem by runing the docker agents with options similar to `--net=host`.

If you have a type(2 or 3) case, please contact us at the migration slack channel (#bazel-migrate-support) and let us know - we want to see the failures and also see if the rbe step also failed this test.

Solutions:
1. If you are using something like `selectRandomPort`:
* Unless you really need to know the port of the server in advance, just do `Server(0)` - this will select a free random port.

If you can provide the server with the ‘0’ port and then later ask again for the port it’s the best approach.

* The only known example where supplying the '0' port is not acceptable is in fw’s test that start a bootstrap-jar in a separate process.

2. If you are doing something else related to networking, consider using 127.0.0.1 or “localhost” instead of InetAddress.getLocalHost

3. jmx/rmi connection errors -  fix by [setting rmi host to listen on localhost](https://github.com/wix-platform/wix-framework/pull/1775) (else defaults to external address when ran inside docker)

### OOM Jenkins errors
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

