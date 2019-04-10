# Troubleshooting Migration failures


The following relates to failures in the `migrate` phase. As a reminder, roughly speaking, the `migrate` phase is concerned with being able to **structurally** understand your repository so it could **successfully** output the bazel build descriptors (`WORKSPACE` file and `BUILD.bazel` files).

## How to use this guide

We recommend to just use `find` on this page every time you have a problem. We've tried incorporating the keywords you'll see when you have a problem so `find` is easier. Reading everything upfront can be counter productive to most people.

----
## Maven Analysis failures
-	Good news are that we haven't found any failures so far.
-	Bad news are that it probably just means we haven't looked at enough crazy maven setups :) If `migrate` fails here we'll need you to tell us about it.

## Transformation failures

###	Code Analysis

**Note:** in the following section file paths are relative to a source dir (`src/it/scala`) as Codota only gets our jars but not access to out VCS. If there is a path then somewhere in the failure stack there is also the `SourceModule` which denotes the source maven module we're looking the file in.

-	**Composite**- Denotes an aggregator of analysis failures
-	**AugmentedFailure**- Denotes a failure which is augmented by some additional metadata along the calling stack. See most inner failure to understand what was the root issue. See *Failure Metadata* section below for more detail on what each augmented metadata means.
-	**SourceMissing**\-
	-	What: Happens when Codota reports a file exists but we can't find it. It might mean the file was renamed, moved or deleted and for some reason it is not reflected in Codota.
	-	Arguments: `filePath: String, module: SourceModule`
	-	What do to:
		-	Verify you're on `master` and haven't moved the file locally
		-	Verify the file is under `src/main`/`src/it`/`src/test`/`src/e2e` and under these it's either `java`/`scala` (e.g. `src/e2e/scala` is supported, `src/foo/java` and `src/main/kotlin` aren't). If you have a different need, talk to us.
		-	Verify Codota received the latest jar (see below `Verifying Codota received your artifact`).
		- 	Note: If you have java/scala files under resources folder (for example for tests), either delete them or mute them in a [source override](overrides.md).
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

###	Graph analysis

-	Cycles - applies if you get an error message which starts with `cycle: Trying to combine` or contains `full subGraph containing cycle`
	-	cyclic dependency between your src/it and src/test- In maven src/it and src/test are the same unit so cyclic dependency between them is not a problem. In bazel it is since we don’t want BUILD.bazel files globbing files from entire source folders. To identify if you have a problem: https://github.com/wix-private/core-server-build-tools/tree/master/scripts/dependency-detector To fix: Usually it’s to move a few test helper classes from src/it to src/test and break the cycle this way
	-	cyclic dependency between your src/it & src/test and cyclic between src/something/java & src/something/scala- In maven src/it and src/test are the same unit so cyclic dependency between them is not a problem. In bazel it is since we don’t want BUILD.bazel files globbing files from entire source folders. Same for cycles between src/something/java and src/something/scala To identify if you have a problem: https://github.com/wix-private/core-server-build-tools/tree/master/scripts/sources-unifier To fix: Use the above tool
-	`can't find resourceKey...`- This probably means there's a bug somewhere. That's what we found the last two times this happened. Talk to us.

## Resolution failures


-	PropertyNotDefinedException- you're depending on a `pom` file which contains incorrect definitions. For example it contains a dependency with a version containing a property but nothing declares the property. You might not have a problem since you don't depend on that specific dependency but our resolver requires the `pom` to be valid. If it's your `pom` then please fix it by removing the faulty entry or adding a property with a default value (if you're not sure how, talk to us).

-	`packaging not supported for`- We currently support only `jar` and `pom` packaging types. If you have a different need please talk to us.


## Verifying Codota received your artifact
- Go to artifactory: (for groupId:some.group artifactId:artifactId version:1.0-SNAPSHOT) https://repo.dev.wixpress.com/artifactory/libs-snapshots/some/group/artifactId/1.0-SNAPSHOT/
- Find the latest sources jar, test-sources.jar, "regular" jar and codota-tests.jar and send all of these filenames to us alongside with your groupId and artifactId. We'll get in touch with codota and check if they received these and whether they were indexed or not.

## Proto dependencies

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
