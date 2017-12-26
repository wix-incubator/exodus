package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.Target.MavenJar
import com.wix.bazel.migrator.model.{AnalyzedFromMavenTarget, ExternalModule, ModuleDependencies, SourceModule, Scope => MavenScope}
import com.wix.bazel.migrator.model.makers.ModuleMaker
import ThirdPartyValidatorTest._
import com.wix.bazel.migrator.model.makers.ModuleMaker.anExternalModule
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ThirdPartyValidatorTest extends SpecificationWithJUnit {

  "Third party validator should return" >> {

    trait ctx extends Scope {
      val someDependency = dependency("some.group", "some-dep", "some-version")
    }

    "no conflicts if given empty set of source modules" in {
      val conflicts = new ThirdPartyValidator(Set.empty, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoConflicts
    }

    "no conflicts if there are no multiple versions in dependencies" in new ctx {
      val someOtherDependency = dependency("some.group", "some-other-dep", "1.0")
      private val sourceModules = Set(
        aModule(artifactId = "module-a", dependencies = Set(someDependency)),
        aModule(artifactId = "module-b", dependencies = Set(someOtherDependency)))

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoFailingConflicts
    }

    "warning conflict if it found more than 1 version of same dependency" in new ctx {
      val someDependencyWithDifferentVersion = someDependency.copy(version = "another-version")
      val moduleADeps = ModuleDependencies()
        .withScopedDependencies(MavenScope.PROD_COMPILE, Set(dontCareDependency)) //ensures we iterate over all scopes
        .withScopedDependencies(MavenScope.TEST_COMPILE, Set(someDependency))
      val moduleBDeps = Set(someDependencyWithDifferentVersion)
      val moduleA = aModule("module-a", moduleADeps)
      val moduleB = aModule("module-b", moduleBDeps)

      val conflicts = new ThirdPartyValidator(sourceModules = Set(moduleA, moduleB),
                                              emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(MultipleVersionDependencyConflict(
        someDependency.groupId,
        someDependency.artifactId,
        Set(
          DependencyWithConsumers(someDependency, Set(moduleA.externalModule)),
          DependencyWithConsumers(someDependencyWithDifferentVersion, Set(moduleB.externalModule)))
      ))
    }

    "no more than 5 consumers per dependency in conflict" in new ctx {
      val someDependencyWithDifferentVersion = someDependency.copy(version = "another-version")
      private val moduleWithExceptionalDependency =
        aModule("exceptional-module", Set(someDependencyWithDifferentVersion))
      val sourceModules = lotsOfModulesThatDependOn(quantity = 100, dependency = someDependency) +
                          moduleWithExceptionalDependency

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      maxConsumersSizeOf(conflicts.warn.head) must beEqualTo(5)
    }

    "only conflicts in third party dependencies (exclude the source module)" in new ctx {
      private val moduleA = aModule(artifactId = "module-a", dependencies = Set.empty[ExternalModule])
      private val moduleB = aModule(artifactId = "module-b", dependencies = Set(moduleA.externalModule))
      private val moduleC = aModule(artifactId = "module-c", dependencies = Set(moduleA.externalModule.copy(version = "some-other-version")))
      private val sourceModules = Set(moduleA, moduleB, moduleC)

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveNoWarningConflicts
    }

    "no warnings conflicts when all dependencies in project are in managed dependencies" in new ctx {
      val moduleA = aModule(artifactId = "module-a", dependencies = Set(someDependency))

      val conflicts = new ThirdPartyValidator(
        sourceModules = Set(moduleA),
        managedDependencies = Set(someDependency)
      ).checkForConflicts()

      conflicts must haveNoWarningConflicts
    }

    "un-managed dependency conflict when dependency of single module project is not in managed dependency" in new ctx {
      val someModule = aModule(artifactId = "module-a", dependencies = Set(someDependency))

      val conflicts = new ThirdPartyValidator(sourceModules = Set(someModule),
                                              emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(UnManagedDependencyConflict(dependencyWithConsumers(someDependency, someModule)))
    }

    "un-managed dependency conflict when dependency of multi-module project is not in managed dependency" in new ctx {
      val moduleA = aModule(artifactId = "module-a", dependencies = Set.empty[ExternalModule])
      val moduleB: SourceModule = aModule(artifactId = "module-b", dependencies = Set(someDependency))
      val sourceModules = Set(moduleA, moduleB)

      val conflicts = new ThirdPartyValidator(sourceModules, emptyManagedDependencies).checkForConflicts()

      conflicts must haveAWarningConflict(UnManagedDependencyConflict(dependencyWithConsumers(someDependency, moduleB)))
    }

    "collision with managed dependency conflict when dependency of project has different version than managed version" in new ctx {
      val moduleWithSomeDependency = aModule(artifactId = "module-a", dependencies = Set(someDependency))
      val someDependencyWithDifferentVersion = someDependency.copy(version = "some-other-version")

      val conflicts = new ThirdPartyValidator(
        sourceModules = Set(moduleWithSomeDependency),
        managedDependencies = Set(someDependencyWithDifferentVersion)
      ).checkForConflicts()

      conflicts must haveAWarningConflict(CollisionWithManagedDependencyConflict(
        dependencyWithConsumers(someDependency, moduleWithSomeDependency),
        someDependencyWithDifferentVersion.version))
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

object ThirdPartyValidatorTest {

  private val emptyManagedDependencies = Set.empty[ExternalModule]

  private val dontCareDependency = dependency("dont.care", "dont-care", "dont-care")

  implicit class ModuleDependenciesExtended(moduleDependencies: ModuleDependencies) {
    def withScopedDependencies(scope: MavenScope, dependencies: Set[ExternalModule]): ModuleDependencies = {
      val dependenciesAsMavenJar: Set[AnalyzedFromMavenTarget] = dependencies.map(aMavenJar)
      moduleDependencies.copy(scopedDependencies = moduleDependencies.scopedDependencies + ((scope, dependenciesAsMavenJar)))
    }
  }

  private def aMavenJar(externalModule: ExternalModule) = MavenJar("dont.care", "dont-care", externalModule)

  private def dependency(groupId: String, artifactId: String, version: String) = anExternalModule(groupId, artifactId, version)

  private def lotsOfModulesThatDependOn(quantity: Int, dependency: ExternalModule): Set[SourceModule] =
    (1 to quantity).map(num => aModule(s"module$num", Set(dependency))).toSet

  private def aModule(artifactId: String, dependencies: Set[ExternalModule]): SourceModule =
    aModule(artifactId, ModuleDependencies().withScopedDependencies(MavenScope.PROD_COMPILE, dependencies))


  private def aModule(artifactId: String, moduleDependencies: ModuleDependencies): SourceModule = {
    ModuleMaker.aModule(anExternalModule(artifactId), moduleDependencies)
  }

  private def dependencyWithConsumers(dependency: ExternalModule, consumers: SourceModule*) =
    DependencyWithConsumers(dependency, consumers.map(_.externalModule).toSet)

}

