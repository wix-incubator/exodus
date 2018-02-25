package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ThirdPartyValidatorTest extends SpecificationWithJUnit {

  "Third party validator should return" >> {

    trait ctx extends Scope {
      val emptyManagedDependencies = Set.empty[Coordinates]

      val baseDependencyCoordinates: Coordinates = someCoordinates("some-dependency")

      val someCompileDependency = Dependency(baseDependencyCoordinates, MavenScope.Compile)

      def lotsOfModulesThatDependOn(quantity: Int, dependency: Dependency): Set[SourceModule] =
        (1 to quantity).map(num => aModule(s"module$num").withDirectDependency(dependency)).toSet

      def coordinateWithConsumers(dependency: Coordinates, consumers: SourceModule*) =
        CoordinatesWithConsumers(dependency, consumers.map(_.coordinates).toSet)
    }

    "no conflicts if given empty set of source modules" in new ctx {
      val conflicts = new ThirdPartyValidator(Set.empty, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoConflicts
    }

    "no conflicts if there are no multiple versions in dependencies" in new ctx {
      val someOtherDependency = Dependency(someCoordinates("other-dep"), MavenScope.Compile)
      private val sourceModules = Set(
        aModule(artifactId = "module-a").withDirectDependency(someCompileDependency),
        aModule(artifactId = "module-b").withDirectDependency(someOtherDependency))

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoFailingConflicts
    }

    "warning conflict if it found more than 1 version of same dependency" in new ctx {
      val someDependencyWithDifferentVersion = someCompileDependency.withVersion("other-version")

      val moduleA = aModule("module-a").withDirectDependency(someCompileDependency)
      val moduleB = aModule("module-b").withDirectDependency(someDependencyWithDifferentVersion)

      val conflicts = new ThirdPartyValidator(sourceModules = Set(moduleA, moduleB),
        emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(MultipleVersionDependencyConflict(
        someCompileDependency.coordinates.groupId,
        someCompileDependency.coordinates.artifactId,
        Set(
          CoordinatesWithConsumers(baseDependencyCoordinates, Set(moduleA.coordinates)),
          CoordinatesWithConsumers(someDependencyWithDifferentVersion.coordinates, Set(moduleB.coordinates)))
      ))
    }

    "no more than 5 consumers per dependency in conflict" in new ctx {
      val someDependencyWithDifferentVersion = someCompileDependency.withVersion("other-version")
      private val moduleWithExceptionalDependency =
        aModule("exceptional-module").withDirectDependency(someDependencyWithDifferentVersion)

      val sourceModules = lotsOfModulesThatDependOn(quantity = 100, dependency = someCompileDependency) +
        moduleWithExceptionalDependency

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      maxConsumersSizeOf(conflicts.warn.head) must beEqualTo(5)
    }

    "only conflicts in third party dependencies (exclude the source module)" in new ctx {
      private val moduleA = aModule(artifactId = "module-a")
      private val moduleB = aModule(artifactId = "module-b").withCompileScopedDependency(moduleA.coordinates)
      private val moduleC = aModule(artifactId = "module-c").withCompileScopedDependency(moduleA.coordinates.copy(version = "some-other-version"))
      private val sourceModules = Set(moduleA, moduleB, moduleC)

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoWarningConflicts
    }

    "no warnings conflicts when all dependencies in project are in managed dependencies" in new ctx {
      val moduleA = aModule(artifactId = "module-a").withDirectDependency(someCompileDependency)

      val conflicts = new ThirdPartyValidator(
        sourceModules = Set(moduleA),
        managedDependencies = Set(someCompileDependency.coordinates)
      ).checkForConflicts()

      conflicts must haveNoWarningConflicts
    }

    "un-managed dependency conflict when dependency of single module project is not in managed dependency" in new ctx {
      val someModule = aModule(artifactId = "module-a").withDirectDependency(someCompileDependency)

      val conflicts = new ThirdPartyValidator(sourceModules = Set(someModule),
        emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(UnManagedDependencyConflict(coordinateWithConsumers(someCompileDependency.coordinates, someModule)))
    }

    "un-managed dependency conflict when dependency of multi-module project is not in managed dependency" in new ctx {
      val moduleA = aModule(artifactId = "module-a")
      val moduleB: SourceModule = aModule(artifactId = "module-b").withDirectDependency(someCompileDependency)
      val sourceModules = Set(moduleA, moduleB)

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(UnManagedDependencyConflict(coordinateWithConsumers(someCompileDependency.coordinates, moduleB)))
    }

    "collision with managed dependency conflict when dependency of project has different version than managed version" in new ctx {
      val moduleWithSomeDependency = aModule(artifactId = "module-a").withDirectDependency(someCompileDependency)
      val someDependencyWithDifferentVersion = someCompileDependency.withVersion("some-other-version")

      val conflicts = new ThirdPartyValidator(
        sourceModules = Set(moduleWithSomeDependency),
        managedDependencies = Set(someDependencyWithDifferentVersion.coordinates)
      ).checkForConflicts()

      conflicts must haveAWarningConflict(CollisionWithManagedDependencyConflict(
        coordinateWithConsumers(someCompileDependency.coordinates, moduleWithSomeDependency),
        someDependencyWithDifferentVersion.version))
    }

    "different exclusion collision when dependency of project has different exclusion managed" in new ctx {

      val sameDependencyWithExclusion = someCompileDependency.withExclusions(Set(Exclusion("some-Group", "some-Artifact")))

      val moduleA = aModule("module-a").withDirectDependency(someCompileDependency)
      val moduleB = aModule("module-b").withDirectDependency(sameDependencyWithExclusion)

      val conflicts = new ThirdPartyValidator(sourceModules = Set(moduleA, moduleB),
        emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(DifferentExclusionCollision(
        someCompileDependency.coordinates.groupId,
        someCompileDependency.coordinates.artifactId,
        Set(DependencyWithConsumers(someCompileDependency, Set(moduleA.coordinates)),
          DependencyWithConsumers(sameDependencyWithExclusion, Set(moduleB.coordinates)))
      ))
    }

  }

  private def haveNoFailingConflicts: Matcher[ThirdPartyConflicts] = failingConflicts ^^ beEmpty

  private def haveNoWarningConflicts: Matcher[ThirdPartyConflicts] = ((_: ThirdPartyConflicts).fail) ^^ beEmpty

  private def haveNoConflicts: Matcher[ThirdPartyConflicts] = haveNoFailingConflicts and haveNoWarningConflicts

  private def haveAWarningConflict(conflict: ThirdPartyConflict): Matcher[ThirdPartyConflicts] = contain(conflict) ^^ warningConflicts

  private def failingConflicts = (_: ThirdPartyConflicts).fail

  private def warningConflicts = { (conflicts: ThirdPartyConflicts) => conflicts.warn }

  private def maxConsumersSizeOf(conflict: ThirdPartyConflict): Int = {
    conflict match {
      case conflict: MultipleVersionDependencyConflict => conflict.versions.map(_.consumers.size).max
      case conflict: UnManagedDependencyConflict => conflict.dependencyAndConsumers.consumers.size
      case conflict: CollisionWithManagedDependencyConflict => conflict.dependencyAndConsumers.consumers.size
    }
  }
}

