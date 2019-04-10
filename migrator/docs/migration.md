Migration
=========

[How it works](how-it-works.md)
-----------------------------------

Troubleshooting
---------------

### [Migration failures](troubleshooting-migration-failures.md)

#### [Analysis failures](troubleshooting-migration-failures.md#maven-analysis-failures)

#### [Transformation failures](troubleshooting-migration-failures.md#transformation-failures)

#### [Resolution failures](troubleshooting-migration-failures.md#resolution-failures)

### [Build failures](troubleshooting-build-failures.md)
custom maven repositores for now unsupported. talk to us and we'll add it to artifactory

#### Compilation failures

annotations with parameters from the same package     
star imports in java     
relative import of constants or tye aliases (`import com.wix.foo.... val a = foo.Bar`) (stuff that doesn't appear in the bytecode)

#### Test failures (in general)

[WIP] Explicit reference to resources: On your tests - do you read directly from path relative to module path? (for instance: `/src/main/resources` or `/src/tests/resources`) - if so you must change your test code to read files from classpath. (code example before and after)

```
// better files
// scala/java path/file
// scala/java stream
```

| Problem | Explanation |
|---------|-------------|
| Glob doesn't cross package boundry | Given the following files: <br> `foo/a.scala` <br>`foo/bar/b.scala`<br>`foo/bar/baz/c.scala`<br>And given `b.scala` doesn’t depend on them and `a.scala` and `c.scala` have a cyclic dependency between them we will have a problem since `foo/bar` will be a package and the migrator will try to create a target in `foo` which will glob the files from `foo` and `foo/bar/baz` but the glob won’t cross the package boundary of `foo/bar` |
| Docker | - |
| Single file test name can cause false positive | If you have a single file in a package and it's name **has** the relevant afix (`TestFooSupport`) but it's not a test you'll need to manually override the migrator result and tell it there are no tests there |
| Single file test name can cause false positive | If you have a single file in a package and it's name **does not have** the relevant afix (`FooTests`) but inside the file contains classes with relevant affixes the migrator will think it's test support and you'll need to manually override the migrator result and tell it there are tests there |

#### UT failures

#### IT/E2E failures

one package with IT and env config, other package with only IT and both of them use xml for the embedded env- solution, move env config to its own package

- environment setup failure, might be caused by MySql/Other server fails to create a listening socket - 
  - search for something similar to:  
  [ERROR] The socket file path is too long (> 107): /dev/shm/bazel-sandbox.3dc26c32fcb1a443450c6f238552bbec/3189251906012878021/execroot/other/_tmp/71a7b77b0712ce3d8c79af47d374a602/5617902258756964527.sock
  - a workaround would be to override JVM param for temp dir name - using the internal overrides - check for example: https://github.com/wix-private/action-triggers-server/tree/master/bazel_migration
  
  

### [Overrides](overrides.md)

### strict deps explanation (warn/error/current java bug and script)
please currently ignore the warnings you see of strict deps and buildozer

### Codota examples
