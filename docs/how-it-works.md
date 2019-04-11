# How It Works

This doc gives a high-level overview of how the migrator turns your Maven project into a Bazel project.

## Maven Analysis

The migration process starts by analyzing your project's current Maven setup. 

During this phase, Exodus analyzes: 

+ The code modules found in your project, ignoring `pom` modules.
+ The dependencies between these code modules. 
+ The resource folders these code modules contain. Out of the default hardcoded: XXX What does this mean? XXX
  + `main/resources`
  + `test\resources`
  + `it\resources`
  + `e2e\resources`

## Internal Code graph

Based on the analysis described above, Exodus creates an internal, fine-grained, code graph using the following process:

1. Query a code index for a list of files and their dependencies for each code module to form a file graph.

    XXX Dependencies here mean other **source files** at Wix<sup>1</sup> your code depends on (i.e. already abstracted over classes to files). XXX  

1. Transform the graphs created above to a package-level graph since we don't want to maintain targets for specific files but aim for package level granularity in the [1:1:1](one target per directory, representing a single package) form. 

1. Remove cycles from the package graph, so it can actually be built, by finding strongly connected components and aggregating cycles to targets of the form `agg=sub_package+other_sub_package`. The aggregator targets are declared in the first shared ancestor.  

    Exodus does not allow cycles between different modules or between the different source directories (it/e2e/test) of a module.

    +  Cycles between modules are not possible if your project builds on maven.
    +  Cycles between modules are usually easy to fix. By not allowing them, resulting targets are more local and don't span over very large source trees.  

1. Translate the above [DAG](https://en.wikipedia.org/wiki/Directed_acyclic_graph) to a Bazel package graph where each [package](https://docs.bazel.build/versions/master/build-ref.html#packages) contains the relevant [targets](https://docs.bazel.build/versions/master/build-ref.html#targets).  

1. Write the package/target graph to Bazel using the relevant [rule](https://docs.bazel.build/versions/master/build-ref.html#rules) names and conventions.

## External Code graph

Bazel does not currently support transitive dependencies of external dependencies (Maven jars as well as other Bazel projects). The following steps expand the transitive graph of your project's dependencies as well as the dependencies defined in the organization's "source of truth" (`third_party_dependencies` parent) so that you can easily depend on new Maven projects without knowing the entire graph.
 
1. Exodus builds a compile and runtime dependency graph of dependencies defined in:

   + Your project.
   + The organization's "source of truth".  

1. Write the above graph out in Bazel-specific form.  

External dependencies are declared in the `WORKSPACE` file in the root of the project and in `BUILD.bazel` files in the `third_party` directory.
