package com.wix.bazel.migrator.model

//TODO relativePathFromMonoRepoRoot String or Path
case class Package(relativePathFromMonoRepoRoot: String, targets: Set[Target], originatingSourceModule: SourceModule)
