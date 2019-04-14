# Overrides

There are several override mechanisms built into Exodus, because even the best heuristics are still just heuristics.

Override files need to be placed in the `bazel_migration` folder in the root of your repository. These files contain serialized JSONs.

## Source File Overrides
Excludes specific source files from migration.

### Why

Override the following files:

+ Java/Scala files without any bytecode.
+ Unsupported files.

### Structure

File path: `bazel_migration/source_files.overrides`

File contents:

```
{
  "mutedFiles": [
    "full/path/to/File.scala",
    "full/path/to/OtherFile.scala"
  ]
}
```
**Note**: The above path is the file path in a module, excluding the source directory `src/main/java`, and can be taken from the Codota error message.

### Example
 
 ```
 {
    "mutedFiles": [
      "jvm-with-packageJson/src/main/scala/a.scala",
      "com/wix/services/TeamcityFacadeTest.scala"
    ]
}
 ```

## Internal Target Overrides
Override many aspects of internal targets.

### Why
Change the properties of the generated target.

### Structure
File path: `bazel_migration/internal_targets.overrides`

File contents:

```
{
  "targetOverrides": [
    {
      "label": "((full-label-of target))",
      attributes...
    }
  ]
}
```

#### Required Properties

- `label` - The fully qualified label of the problematic target in Bazel.

#### Optional Attributes

- `testOnly` - Set to `true` if you have testkit code which sits under `src/main` and depends on some test target.
- `testType`- Changes the type of test target. This is needed if there is a mismatch between the filename and classes in it. 

    For example, if a file is called `FooTest` but it contains `Foo1IT` and `Foo2IT`. The tests won't run because Exodus instructs Bazel to only run classes which end with `Test`. Override it to `ITE2E` or `Mixed` to have all of them run. 
    
    Possible values (case sensitive):
	
    - `UT` - Unit tests.
	- `ITE2E` - Integration or e2e tests.
	- `Mixed` - Package with both tests.
	- `None` - Test support code with no actual tests to run.

-	`testSize` - Configure the test size of the target. The test size limits the test suite timeout and resource allocation. 

    Possible values: as described [here](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes-tests)
-	`testTypeOnlyForTags` - Modifies the tags of the target with respect to test type. Impacts whether the test is run in the `UT` phase or `IT/E2E` phase. 

    For example, some unit tests of embedded mongo-test-kit need this. Instead of using this override, it is recommended that you change the unit test to be an `IT/E2E` test by changing the filename suffix/prefix **and** changing the test class name suffix/prefix to `IT/E2E`.
-	`additionalJvmFlags` - List of additional jvm flags. For example, custom `java.io.tmpDir`.
-	`additionalDataDeps` - List of additional data dependencies. For example, depend on an embedded MySQL installer.
-	`newName` - New name for your target. This is not recommended but sometimes needed. 

    For example, if you have a big cycle that will make the filename too long).
    
    ```json
    {
      "targetOverrides": [
        {		
            "label": "//save-the-poor-dolphin/src/main/scala/com/wixpress/elvisisalive:agg=a+very+very+very+very+very+very+long+name",
            "newName": "a-shorter-name"
        }
      ]
    }
    ```
-	`additionalProtoAttributes` - Additional attributes to be added to a proto target. For example to be used with additional generators. See example below.

#### Simplified Example:

```json
{
  "targetOverrides": [
    {
      "label": "//bi-modules/wix-page-view-reporting-testapps/wix-page-view-reporting-bootstrap-testapp/src/it/scala/com/wixpress/bi:bi",
      "testType": "ITE2E",
      "testSize": "large",
      "tags": "ITE2E",
      "additionalJvmFlags": [
        "-Djava.io.tmpdir=/tmp"
      ],
      "additionalDataDeps": [
        "@mongo_default_version//:binary"
      ]
    }
  ]
}
```

#### Additional Proto Attributes Example:

For `accord` and `call_scope` generators use this example.

Note, you need to escape the content. Use an [online escaping tool](https://www.freeformatter.com/json-escape.html) to assist

```json
{
  "targetOverrides": [
    {
      "label": "//foo/bar/src/main/proto:proto",
      "additionalProtoAttributes": "additional_generators = {\r\n        \"accord\": [\r\n            \"@com_wix_accord_api_2_12//jar\",\r\n            \"@com_wix_accord_core_2_12//jar\"\r\n            ],\r\n        \"call_scope\": [],\r\n    },"
    }
  ]
}
```

The result will be something like:

```
wix_scala_proto_library(
  name = "name_scala",
  deps = [":name"],
  visibility = ["//visibility:public"],
  additional_generators = {
    "accord": [
      "@com_wix_accord_api_2_12//jar",
      "@com_wix_accord_core_2_12//jar"
    ],
    "call_scope": [],
  },
)
```
- `dockerImagesDeps` - list of Docker images the target depends on. 

    In the form of: 
    
    - `registry/repository:tag`
    - `repository:tag` This short form can be used  for common 3rd party images in the standard docker registry.
    
    See [this](docker.md) document for use case.

    **Important:** Do _not_ use tags such as `snapshot` and `latest`. These do not behave like Maven snapshots and have no special meaning to Bazel. They will only confuse you. Use explicit version tags instead.

    For example:
    ```json
    {
      "targetOverrides": [
        {
          "label": "//bi-modules/wix-page-view-reporting-testapps/wix-page-view-reporting-bootstrap-testapp/src/it/scala/com/wixpress/bi:bi",
          "dockerImagesDeps":["docker-repo.wixpress.com/com.wixpress.stuff:1.23.0", "mysql:5.7", "redis:4.0.11"]
        }
      ]
    }
    ```

## Third Party Overrides

Third party target dependencies on internal targets.

### Why

- During third party resolution, Exodus filters the Maven jars of the current project because they should be built using Bazel from the source code.
- Since each Maven jar is broken into multiple bazel targets, Exodus cannot know which Bazel targets should replace the Maven jar dependency, and Exodus doesn't want to lose the fine granularity.

### Structure
File path: `bazel_migration/third_party_targets.overrides`

File contents: 

Map of (third-party `groupId:ArtifactId`) -> (list of Bazel targets)

```json
{
  "compileTimeOverrides": {
    "<groupId>:<artifactId>": [
      "label1", "label2"
    ]
  },
  "runtimeOverrides": {
    "<groupId>:<artifactId>": [
      "label3","label4"
    ]
  }
}
```

**Notes:** 
- Labels are Bazel labels in the format of: `//some/package:target_name`.
- You will probably need to go through 1 iteration of build failures to know the Bazel labels of your code.

### Example:

When migrating `wix-framework` we found dependency on `wix-petri` which had dependency on koboshi (which is built inside framework). 

This is the overrides file:

```json
{
  "compileTimeOverrides": {
    "com.wixpress.common:wix-petri-testkit": [
      "//koboshi-modules/wix-koboshi-testkit/src/main/java/com/wixpress/framework/koboshi/it:it",
      "//test-infrastructures-modules/legacy-java-http-client/src/main/java/com/wixpress/framework/test/apiClient:apiClient",
      "//configuration-modules/hoopoe-config-testkit/src/main/scala/com/wixpress/hoopoe/config:config"
    ],
    "com.wixpress.common:wix-petri-integration": [
      "//koboshi-modules/wix-koboshi/src/main/java/com/wixpress/framework/koboshi/spring/mvc:mvc"
    ]
  }
}
 ```

## Internal File Dependency Overrides
Add dependencies between internal files. 

### Why
- Codota's Index isn't bulletproof and sometimes explicit overrides are needed.
- Currently, for **compile-time**, Exodus only knows about problems with respect to star imports, relative imports, and same package in different module (no imports) of constants and annotation parameters.
- There are situations where the bytecode has no evidence and the imports aren't explicit.
- You find another use-case. **Please don't apply this blindly. This might be a bug we can solve, benefiting everyone.**   
- For **runtime**, Exodus knows about XMLs, such as logback and spring, that depend on classes for example custom appenders.
- Note that every Java/Scala target has an automatic dependency on the resources folder in its source dir (e.g. `src/it/resources`). If you add a dependency from an XML to a Java/Scala file in the same source folder you might create a cycle. The solution is to usually add a dependency to the class **using** the XML instead of the XML itself. This might mean adding the dependency multiple times (for multiple targets) instead of once to the XML, but that's the workaround.     
- Another alternative is to try and switch to `LogbackTestHelper` and the `GlobalTestEnv` from the FW and code based spring config since they encode this dependency into compile time. 

### Structure
File path: `bazel_migration/internal_file_deps.overrides`

File contents:

Map of (`relative path to module` ) -> (map of (`relative path of using code in module`) -> list (`relative path of dependency`))

```json
{
  "runtimeOverrides" : {
    "relative-path/to/module1" : {
      "src/main/java/path1/file1.java" : [ "relative-path/to/module1/src/main/scala/path/to/dependency/runtime1.scala"],
      "src/main/scala/path2/file2.java" : [ "other/module/src/main/scala/path/to/dependency/runtime2.scala"]
    },
    "relative-path/to/module2" : {
      "src/test/scala/path1/file1.java" : [ "other/module/src/main/scala/path/to/dependency/runtime1.scala"]
    }
  },
  "compileTimeOverrides" : {
    "relative-path/to/module1" : {
      "src/main/scala/path1/file1.java" : [ "other/module/src/main/scala/path/to/dependency/compile1.scala"]
    }
  }
}
```
**Note:** For dependencies you should always provide the full path relative to the repo root.

## WORKSPACE Overrides
Add non-default repository rule targets at the end of the WORKSPACE file.

### Why
- There are cases where some parts of the repository have already started using Bazel without also defining an analogous Maven configuration.
- The Bazel configuration may depend on repositories not defined by Exodus by default.
- One such example is test code that needs to run Bazel itself and is therefore dependant on the `bazelbuild/bazel-integration-testing` repository. 

**Note:** Before using this override, please contact us so we'll make sure the use-case is indeed specific. If it's generic we want to add it to everyone's WORKSPACE.

### Structure
File path: `bazel_migration/workspace.suffix.overrides`

#### Example:

Depending on `bazelbuild/bazel-integration-testing` repository:

```
build_bazel_integration_testing_version="36ffe6fe0f4bb727c1fe34209a8d6fd33d8d0d8e" # update this as needed

http_archive(
  name = "build_bazel_integration_testing",
  url = "https://github.com/bazelbuild/bazel-integration-testing/archive/%s.zip" % build_bazel_integration_testing_version,
  strip_prefix = "bazel-integration-testing-%s" % build_bazel_integration_testing_version,
)
```
