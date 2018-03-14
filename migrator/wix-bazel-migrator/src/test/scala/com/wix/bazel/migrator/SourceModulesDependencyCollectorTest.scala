package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.ModuleDependencies
import com.wix.bazel.migrator.model.makers.ModuleMaker.{ModuleExtended, aModule}
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import org.specs2.mutable.SpecificationWithJUnit

class SourceModulesDependencyCollectorTest extends SpecificationWithJUnit {

  val collector = new SourceModulesDependencyCollector(new DependencyCollector())

  "CodeModulesCollector" should {
    "take highest version on collision of direct dependencies" in {
      val dependency = aDependency("some-dependency").withVersion("1")
      val dependencyWithDiffVersion = dependency.withVersion("2")

      val moduleA = aModule(artifactId = "module-a").withDirectDependency(dependency).withAllDependencies(dependency)
      val moduleB = aModule(artifactId = "module-b").withDirectDependency(dependencyWithDiffVersion).withAllDependencies(dependencyWithDiffVersion)

      val dependencies = collector.collectExternalDependenciesUsedBy(Set(moduleA, moduleB))

      dependencies must containTheSameElementsAs(Seq(dependencyWithDiffVersion))
    }

    "take highest version on collision of transitive dependencies" in {
      val a = aDependency("a")
      val b = aDependency("b")

      val moduleA = aModule(artifactId = "module-a").withDirectDependency(a).withAllDependencies(a, b.withVersion("1"))
      val moduleB = aModule(artifactId = "module-b").withDirectDependency(a).withAllDependencies(a, b.withVersion("3"))

      val dependencies = collector.collectExternalDependenciesUsedBy(Set(moduleA, moduleB))

      dependencies must contain (b.withVersion("3"))
      dependencies must not contain b.withVersion("1")
    }

    "prefer versions of direct dependencies" in {
      val a = aDependency("a")
      val t = aDependency("t")

      val moduleA = aModule(artifactId = "module-a").withDirectDependency(a.withVersion("1")).withAllDependencies(a.withVersion("1"))
      val moduleB = aModule(artifactId = "module-b").withDirectDependency(t).withAllDependencies(t, a.withVersion("3"))

      val dependencies = collector.collectExternalDependenciesUsedBy(Set(moduleA, moduleB))

      dependencies must contain (a.withVersion("1"))
      dependencies must not contain a.withVersion("3")
    }

    "collect exclusions from any instance of the dependency" in {
      val a = aDependency("a")
      val b = aDependency("b")
      val c = aDependency("c")

      val aExcludesB = a.withExclusions(Set(Exclusion(b)))
      val moduleA = aModule(artifactId = "module-a").withDirectDependency(aExcludesB).withAllDependencies(aExcludesB)
      val moduleB = aModule(artifactId = "module-b").withDirectDependency(b).withAllDependencies(b, a.withExclusions(Set(Exclusion(c))))

      val dependencies = collector.collectExternalDependenciesUsedBy(Set(moduleA, moduleB))

      dependencies must contain (a.withExclusions(Set(Exclusion(b), Exclusion(c))))
    }

    "remove other modules dependencies" in {
      val a = someCoordinates("module-a")
      val moduleA = aModule(a, ModuleDependencies())
      val moduleB = aModule("module-b").withDirectDependency(aDependencyFor(a)).withAllDependencies(aDependencyFor(a))

      val dependencies = collector.collectExternalDependenciesUsedBy(Set(moduleA, moduleB))
      dependencies must not contain aDependencyFor(a)
    }
  }
}