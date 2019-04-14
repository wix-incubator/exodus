# Exodus - Maven to Bazel Migration

Thinking about migrating from Maven or similar to another build system? This is our story at Wix. For some more background info, take a look at our [blog articles on Medium](https://medium.com/wix-engineering/migrating-to-bazel-from-maven-or-gradle-5-crucial-questions-you-should-ask-yourself-f23ac6bca070).

## Maven had been good to us...for a time

Like many Java or Scala developers, you have probably been using Apache Maven to build your projects. We got used to working with POM (project object model) files and waiting for our projects to build, hoping we had no errors and those pesky dependencies would all work out. 

Maven was good to us when our repositories were a certain size and had less complex dependencies. Maven builds took hours to finish in CI on any infrastructure code change.

At Wix we realized we needed a new build manager.

## Why Bazel

After evaluating several solutions, **Bazel** was chosen as the best alternative build tool. 

### Faster builds and tests
The most significant advantage of Bazel is the speed of the build process. What used to take us hours now takes us minutes.

How does Bazel perform so fast?
It uses: 
* Caching - previous results and meta data are cached so they can be skipped on subsequent builds.
* Parallelism - can run compilations and tests in parallel.
* Hermeticity - builds can access only what is declared and pre-fetched, so they run 'hermetically' without unnecesary noise from additional files. 

### Multi-language environment
Here at Wix, we support different software languages and it's a huge advantage to be able to build multiple languages simultaneously. Currently we migrated JVM languages, but we are thinking of migrating node.js and maybe even React as well. 

### Scalability
Bazel can handle multiple repositories or any size mono-repo. We are able to scale our codebase and our continuous integration system. We use a build workers farm (read about Bazel remote execution) and can scale out build actions to hunderds of workers simultaneously. This means we have room to grow our code which is already at around 10 million lines of code in JVM languages

### Extend languages and platforms
Bazel enables us to extend to more languages and platforms using Bazel's familiar extension language. Because there is a growing community of Bazel uses, we can share and re-use language rules written by other teams. We maintain and contribute to [rules_scala](https://github.com/bazelbuild/rules_scala), a large thriving community of rules contributers.

## How to migrate
So now that you're considering migrating from Maven to Bazel, you may be intimated by the manual migration provided in the Bazel [documentation](https://docs.bazel.build/versions/master/migrate-maven.html). 

Good news! We at Wix have created Exodus, an automated migration tool, and are providing you the files and documentation you need to more easily perform the migration.

Links to repo & How to docs.

