How it works
============

The migrator is composed of a few smaller parts which aim to take a maven codebase and migrate it, with package level granularity, to Bazel.  
It's important to understand that the migrator does a very good job at automating the migration but false positives and negatives exists which is why we have [override mechanisms](overrides.md) in place.

Maven Analysis
--------------

It starts with analyzing the current build system, `maven`. It understands which code modules (non `pom` modules) your project has, which dependencies they have and which resource folders they contain (out of the default hardcoded `main/resources` \ `test\resources` \ `it\resources` \ `e2e\resources`).

Internal Code graph
-------------------

Given the above it will create the internal, fine-grain, code graph by:  
1. Querying the code index (Codota) for the list of files and their dependencies each code module has to form a file graph. Dependencies here mean other **source files** at Wix<sup>1</sup> your code depends on (i.e. already abstracted over classes to files).  
2. Given the above file graph we actually transform it to a package level graph since we don't want to maintain targets for specific files but aim for package level granularity in the [1:1:1](one target per directory, representing a single package) form.  
3. Then we remove cycles in the package graph, so it can actually be built, by finding strongly connected components and aggregating cycles to targets of the form `agg=sub_package+other_sub_package`. The aggregator targets are declared in the first shared ancestor<sup>2</sup>.  
4. We now translate the above [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph) to a Bazel-Package<sup>3</sup> graph where each package contains the relevant targets.  
5. Finally we write the above package/target graph to Bazel using the relevant rule names and conventions.

External Code graph
-------------------

Bazel currently does not support transitive dependencies of external dependencies (maven jars as well as other Bazel projects). The following step expands the transitive graph of your project's dependencies as well as the dependencies defined in the organization's source of truth (`third_party_dependencies` parent) so that you can easily depend on new maven projects without knowing the entire graph.  
It does so by building a compile and runtime dependency graph of dependencies defined throughout your project and from dependencies in the org's "source of truth".  
Given we have resolved this graph we now write it out in Bazel specific form.  
External dependencies are declared in the `WORKSPACE` file in the root of the project and in `BUILD.bazel` files under `third_party` directory.

Footnotes
---------

[1] We **currently** only refer to other source files in your project but next migration phase will utilize source file dependencies across projects at Wix.  
[2] For hygiene reasons we disallow cycles between different modules and between different source directories (it/e2e/test) of a module. cycles between modules can't happen if your build builds on maven and source-dir cycles are usually easy to fix. The gain is that targets are much more local and don't span over very large source trees.  
[3] In Bazel roughly speaking a package is a collection of targets where a target is usually the core building unit. A target can be building a specific library, packaging a deployable or running tests. The above targets generate actions like compilation, archive creation and more which are what the build tool does. For a more formal definition see the [Bazel site](https://docs.bazel.build/versions/master/build-ref.html#packages).
