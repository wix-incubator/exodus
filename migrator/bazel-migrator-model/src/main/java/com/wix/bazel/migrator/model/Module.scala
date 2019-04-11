package com.wix.bazel.migrator.model

import com.wixpress.build.maven.{Coordinates, Dependency}

case class SourceModule(relativePathFromMonoRepoRoot: String,
                        coordinates: Coordinates,
                        resourcesPaths: Set[String] = Set.empty,
                        dependencies: ModuleDependencies = ModuleDependencies(),
                       )

case class ModuleDependencies(directDependencies: Set[Dependency] = Set.empty,
                              allDependencies: Set[Dependency] = Set.empty)

//Omits version since source dependency
//Omits packaging and classifier since they are very hard to generalize
case class DependencyOnSourceModule(relativePath: String, isDependingOnTests: Boolean = false)
