# How to keep your branch green

Want to get notified on any regression?
Add the file `slack_channels.txt` with a name of your team's slack channel (or comma separated list of channels) to `bazel_migration` folder of your repository.

## General Guidelines
1) Do not leave empty (or fully commented out) source files in the repository as this will fail the bazel build
2) If you delete/renmae a maven module, or completely remove its test/it/e2e folders (so that it doesn't have a test jar) - let us know ASAP so we can update codota about it.
3) Make sure your build always passes on Teamcity - otherwise it won't be shipped to codota.
4) Avoid loading symbols using components scan - rather have excplit reference to symbols you are using.
5) Avoid cyclic dependencies between packages within the same module (in the same way it is not possible to create cyclic dependencies between maven modules) 

## Testing Guidelines
1) `IT`/`Test`/`E2E` prefix/suffix are hints for executable tests. Avoid using this pattern for anything that is not an executable test class (like TestEnv or abstract test classes)
2) `IT` or `E2E` prefix/suffix should be used for tests that uses filesystem / network.
3) When creating New tests that use EmbeddedMySql or EmbeddedMongo, expect migration failure and fix it with overrides according to migration docs.
4) Never try to write files to a static folder.
5) Never refer to files from relative maven module path like `src/main/resources` or `src/test/resources`. Resource files should be loaded using classpath loader.
6) Exception to rule #4 is to dynamically generate files that should be loaded to classpath. We hacked our test runner to have the folder `target/test-classes` and add it to test runtime classpath.
7) Test classes should extend `org.specs2.mutable.SpecificationWithJUnit`. Do not extend `org.specs2.mutable.Specification`.
