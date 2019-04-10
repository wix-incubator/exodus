# Overrides

We have several override mechanisms built into the migrator since we know that even best heuristics are still heuristics.

All override files need to be placed under `bazel_migration` folder in the root of your repository and are simply serialized jsons.

## SourceFilesOverrides
Exclude specific source files from migration

#### Why
A java/scala file without bytecode, Currently unsupported file ~(proto for example)~ [proto supported]

### structure
File location: `bazel_migration/source_files.overrides`
```
{"mutedFiles": ["full/path/to/File.scala","full/path/to/OtherFile.scala"]}
```
**Important Notes**: The path above is the file path in a module (excluding source-dir `src/main/java`) and can be taken from the Codota error message.

#### Example
 ```
 {"mutedFiles": ["jvm-with-packageJson/src/main/scala/a.scala","com/wix/services/TeamcityFacadeTest.scala"]}
 ```

## InternalTargetsOverrides
Override many aspects of internal targets

#### Why
Change the properties of generated target to be something else

#### structure
File location: `bazel_migration/internal_targets.overrides`
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
-	`label` is the fully qualified label of the problematic target in Bazel.
##### Possible attirbutes:
All attributes are optionals
- `testOnly` - Configure to `true` if you have testkit code which sits under `src/main` and depends on some test target.
-	`testType`- change the type of test target. This is needed if there is a mismatch between the filename and classes in it. For example the file is called `FooTest` but in it you have `Foo1IT` and `Foo2IT`. The tests won't run because the migrator will instruct bazel to run classes which end with `Test`. Override it to `ITE2E` or `Mixed` to have all of them run. Possible values (CASE SENSITIVE):
	- `UT` - for unit tests
	- `ITE2E` - for integration or e2e tests
	- `Mixed` - for package with both tests
	- `None` - for test support code (no actual tests to run)
-	`testSize`- Configure the test size of the target. Impacts test suite timeout and resources allocation ([see here](https://docs.bazel.build/versions/master/be/common-definitions.html#common-attributes-tests) possible values)
-	`testTypeOnlyForTags`- Modify the tags of the target with respect to test type. Impacts if this test is run in the UT phase or IT/E2E phase. For example some unit-tests of embedded mongo-test-kit need this. This is better avoided in favor of moving the unit test to be an IT/E2E (by changing the filename suffix/prefix **and** changing the test-class name suffix/prefix to IT/E2E).
-	`additionalJvmFlags`- List of additional jvm flags, for example custom `java.io.tmpDir`.
-	`additionalDataDeps`- List of additional data dependencies, for example depend on embedded mysql installer.
-	`newName`- New name for your target. Not recommended but sometimes mandatory (for example if you have such a big cycle that the filename too long). for example:
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
-	`additionalProtoAttributes`- Additional attributes to be added to a proto target. For example to be used with additional generators. See example below.

#### Very synthentic Example:
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

#### additionalProtoAttributes example:
If you need `accord` and `call_scope` generators use the below example.
Note you need to escape the content so if you have other needs use an [online escaping tool](https://www.freeformatter.com/json-escape.html) to assist
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
- `dockerImagesDeps` - list of docker images the target depends on, in the form `registry/repository:tag`, or for common 3rd party images in the standard docker registry, the short form `repository:tag` is also acceptable. See [this readme](docker.md) for use case.

**Important:**
Do _not_ use tags such as `snapshot` and `latest`. These do not behave like maven snapshots anyway and have no special meaning to Bazel, so they will only confuse you. Use explicit version tags.

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

## ThirdPartyOverrides
Add to third party targets dependencies on internal targets

#### Why
- During third party resolution we filter our maven jar of the current project (because they should be built using Bazel from source code)
- Since each maven jar is broken to multiple bazel targets we cannot know which bazel targets should replace the maven jar dependency, and we don't want to lose the fine granularity

#### Structure
Filename: `bazel_migration/third_party_targets.overrides`

#### Format:
map of (`groupId:ArtifactId` of the third party) -> (list of bazel targets)
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

**Note1:** labels are bazel labels in format of `//some/package:target_name`

**Note2:** You will probably need to go through 1 iteration of build failures in order to know the bazel labels of your code

#### Example:
When migrating `wix-framework` we found dependency on `wix-petri` which had dependency on koboshi (which is built inside framework). This is the overrides file:
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
Add dependencies between internal files 

#### Why
- Codota's Index isn't bullet proof and sometimes explicit overrides are needed
- Currently for **compile-time** we only know about problems with respect to star imports, relative imports and same package in different module (no imports) of constants and annotation parameters.
- Simply put this is in a situation where the bytecode has no evidence and the imports aren't explicit
- If you find another use-case **please don't apply this blindly and talk to us since this might be a bug we can solve in everyone's interest**   
- For **runtime** we know about XMLs, such as logback and spring, that depend on classes (for example custom appenders).    
- Note that every java/scala target has an automatic dependency on the resources folder in its source dir (`src/it/resources` for example). If you add a dependency from an XML to a java/scala file in the same source folder you might create a cycle. The solution is to usually add a dependency to the class **using** the XML instead of the XML itself. This might mean adding the dependency multiple times (for multiple targets) instead of once to the XML but that's the workaround.     
- Another alternative is to try and switch to `LogbackTestHelper` and the `GlobalTestEnv` from the FW and code based spring config since they encode this dependency into compile time. 

#### Structure
Filename: `bazel_migration/internal_file_deps.overrides`

#### Format:
map of (`relative path to module` ) -> (map of (`relative path of using code in module`) -> list (`relative path of dependency`))
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
**Note:** For dependencies you should always provide the full path (relative to the repo root)

## WORKSPACE overrides
Add non-default repository rule targets at the end of the WORKSPACE file

#### Why
- There can be cases where some parts of the repository have already started using bazel without also defining analogous maven configuration
- This bazel configuration may depend on repositories not defined by migrator by default
- one such example is test code that needs to run bazel itself and thus is dependant on `bazelbuild/bazel-integration-testing` repository 

**Note:** Before using this override please contact us so we'll make sure the use-case is indeed specific. since if it's generic we want to add it to everyone's WORKSPACE

#### Structure
Filename: `bazel_migration/workspace.suffix.overrides`

#### Example:
depending on `bazelbuild/bazel-integration-testing` repository
```
build_bazel_integration_testing_version="36ffe6fe0f4bb727c1fe34209a8d6fd33d8d0d8e" # update this as needed

http_archive(
  name = "build_bazel_integration_testing",
  url = "https://github.com/bazelbuild/bazel-integration-testing/archive/%s.zip" % build_bazel_integration_testing_version,
  strip_prefix = "bazel-integration-testing-%s" % build_bazel_integration_testing_version,
)
```
