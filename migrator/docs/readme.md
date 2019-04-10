# THE MIGRATOR'S GUIDE TO BAZEL GALAXY
## Backgroud
You probably heard about [bazel](http://bazel.build) - Google's open source build tool that is both **fast** and **accurate**.
Maven was okay, but at one point - using maven to build and test Wix's project became a very very heavy task.

After evaluating several solutions, **Bazel** was chosen as the best alternative build tool that would allow us to get a much faster builds

This guide will guide you (or link you :smile: ) to how to work with bazel and how to verify that your maven repository is migrated well

## Table of content
- [TL;DR - what do I need to do?](#tldr---what-do-i-need-to-do)
- [Links for bazel newbies](#links-for-bazel-newbies)
	- [bazel basics](#bazel-basics)
	- [preparing local environment](#preparing-local-environment)
	- [workshops](#workshops)
	- [other links](#other-links)
- [So how will migration work?](#so-how-will-migration-work)
- [How to work with Jenkins](#how-to-work-with-jenkins)
- [Aditional guides](#aditional-guides)
	- [Migration](#migration)
	- [Bazel compile / test issues](#bazel-compile-test-issues)

## TL;DR - what do I need to do?
 1. Find your project migration page on [jenkins](http://ci-jenkins-poc0a.42.wixprod.net/) (VPN is required)
 2. Run the step **00-Run all Migration steps**
 3. Iteratively fix build failures until all steps complete!
     * See the [Troubleshooting Guide](troubleshooting-migration-failures.md) for more information

When everything is green, this means the migration succeeded, and all tests were executed.

<img src="https://user-images.githubusercontent.com/601206/35227758-94845232-ff97-11e7-8245-9e57efaf5653.png" width=450 alt="jobs" />

### Migration steps details

Complete migration performs the following steps:

* Migrates existing maven project(s) to bazel
  * Creates a separate branch named `bazel-mig-{number}`
  * Pushes all migration changes to this branch
* Builds the newly-migrated bazel project(s)
  * Runs the maven build in parallel, to collect test information (junit xml files)
* Performs a comparison between bazel test results and maven
  * If no differences found, reports success
  * Otherwise fails comparison, prints the differences to the console output

## Links for bazel newbies
### bazel basics
* [Bazel Basic Terminology - Wix slides](https://docs.google.com/a/wix.com/presentation/d/1Pvki8Ve5CP-NliEWDPOjRKAes4qMsIGAPCzurMb8VJs/edit?usp=sharing)
* [Concepts and Terminology - Google](https://docs.bazel.build/versions/master/build-ref.html)
* [Bazel architecture - Wix Slides](https://docs.google.com/a/wix.com/presentation/d/1LPBblEqRm_ikXYTjAhOii1IL3nHDMdzFx-a0dF1QHsk/edit?usp=sharing)
* [Bazel User Manual - Google](https://docs.bazel.build/versions/master/user-manual.html)

### preparing local environment
* [Installing Bazel - Google](https://docs.bazel.build/versions/master/install.html)
* [Bazel in Intellij - Wix doc](https://docs.google.com/a/wix.com/document/d/1nd2OodffEIr676o_oKRF7kWJ5s83lR_FvAp_f698MUw/edit?usp=sharing)

### workshops
* [Bazel 101 workshop - Wix](https://github.com/natansil/bazel-101-workshop)

### other links
* [bazel blog - Google](https://blog.bazel.build/)

## Working with Jenkins
[see here](https://docs.google.com/a/wix.com/document/d/1G1tgM52ZkMLByc10ZG6qV1V81cXU34f5KdiZzEKGKtQ/edit?usp=sharing)

## Aditional guides
### Migration
* [general info](migration.md)
* [troubleshooting](troubleshooting-migration-failures.md)

### Bazel compile / test issues
* [troubleshooting bazel run (WIP)](troubleshooting-build-failures.md)
* [running-bazel-locally](running-bazel-locally.adoc)

### Docker with Bazel
* [Using docker for testing in Bazel builds](docker.md)

