# Bazel Basics

Here are some basic principles about Bazel that are good to know before you start the migration.

## WORKSPACE and BUILD files 
Bazel builds software from a directory called a workspace. Each workspace directory has a text file named `WORKSPACE` which contain references to external dependencies required to build the project, or it may be empty.

Source code is organized in a nested hierarchy of packages. Each package is a directory that contains a set of related source files plus one BUILD file. You create this BUILD file for each package and it specifies what software outputs can be built from the source code. The other elements in the package directory can be either:
- files
- rules

Here are some [guidelines](https://medium.com/wix-engineering/migrating-to-bazel-from-maven-or-gradle-part-1-how-to-choose-the-right-build-unit-granularity-a58a8142c549) about how to determine your granularity level for your build processes from our [series of blog articles on Medium](https://medium.com/wix-engineering/migrating-to-bazel-from-maven-or-gradle-5-crucial-questions-you-should-ask-yourself-f23ac6bca070).

### Example of a directory hierarchy
```
Scala_project
├── WORKSPACE
└── src
    ├── main
       └── scala
    │       └── com
    │           └── example
    │               ├── A.scala
    │               ├── B.scala
    │               ├── BUILD
    │               ├── C.scala
    │               └── Example.scala
    └── test
        └── scala
            └── com
                └── example
                    ├── ExampleTest.scala
```

### Advantage over Maven
In contrast, Maven used one `pom.xml` file that controlled an entire build process. The way Bazel divides the build process into these distinct packages, makes the build process more efficient because you control the granularity of the package to be built. 
