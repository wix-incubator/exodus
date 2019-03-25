package com.wixpress.build.sync

import com.wixpress.build.maven.MavenMakers._
import org.specs2.mutable.SpecificationWithJUnit

class ConflictReportCreatorTest extends SpecificationWithJUnit {
  "ConflictReportCreator" should {
    "report on new local version higher then previous one" in {
      val dependency = randomDependency(withVersion = "2")
      report.report(DiffResult(
        updatedBazelLocalNodes = Set(aRootBazelDependencyNode(dependency)),
        preExistingLocalNodes = Set(aRootDependencyNode(dependency.withVersion("1"))),
        managedNodes = Set())
      ) mustEqual UserAddedDepsConflictReport(Set(HigherVersionThanBefore(dependency.coordinates, "1")))
    }

    "not report on completely new unmanaged artifact" in {
      val dependency = randomDependency()

      report.report(DiffResult(
        updatedBazelLocalNodes = Set(aRootBazelDependencyNode(dependency)),
        preExistingLocalNodes = Set(),
        managedNodes = Set())) mustEqual UserAddedDepsConflictReport()
    }

    "report on multiple higher version conflicts" in {
      val dependencyA = randomDependency(withVersion = "2")
      val dependencyB = randomDependency(withVersion = "4")
      report.report(DiffResult(
        updatedBazelLocalNodes = Set(aRootBazelDependencyNode(dependencyA), aRootBazelDependencyNode(dependencyB)),
        preExistingLocalNodes = Set(aRootDependencyNode(dependencyA.withVersion("1")), aRootDependencyNode(dependencyB.withVersion("3"))),
        managedNodes = Set())) mustEqual UserAddedDepsConflictReport(Set(
        HigherVersionThanBefore(dependencyA.coordinates, "1"),
        HigherVersionThanBefore(dependencyB.coordinates, "3")))
    }

    "do no report on new local version lower then previous one" in {
      val dependency = randomDependency(withVersion = "1")
      report.report(DiffResult(
        updatedBazelLocalNodes = Set(aRootBazelDependencyNode(dependency)),
        preExistingLocalNodes = Set(aRootDependencyNode(dependency.withVersion("2"))),
        managedNodes = Set())
      ) mustEqual UserAddedDepsConflictReport()
    }

    "report on new local artifact with different managed version" in {
      val dependencyA = randomDependency(withVersion = "2")
      val dependencyB = randomDependency(withVersion = "1")
      val managedDepA = dependencyA.withVersion("1")
      val managedDepB = dependencyB.withVersion("2")
      report.report(DiffResult(
        updatedBazelLocalNodes = Set(aRootBazelDependencyNode(dependencyA), aRootBazelDependencyNode(dependencyB)),
        preExistingLocalNodes = Set(),
        managedNodes = Set(aRootDependencyNode(managedDepA), aRootDependencyNode(managedDepB)))
      ) mustEqual UserAddedDepsConflictReport(differentManagedVersionConflicts = Set(
        DifferentManagedVersionExists(dependencyA, managedDepA),
        DifferentManagedVersionExists(dependencyB, managedDepB)))
    }

    "not report on unchanged local artifact with different managed version" in {
      val dependency = randomDependency(withVersion = "2")
      val dependencyNodes = Set(aRootBazelDependencyNode(dependency))
      report.report(DiffResult(
        updatedBazelLocalNodes = dependencyNodes,
        preExistingLocalNodes = dependencyNodes.map(_.toMavenNode),
        managedNodes = Set(aRootDependencyNode(dependency.withVersion("1"))))
      ) mustEqual UserAddedDepsConflictReport()
    }

    "report nothing when no updatedLocalDeps exists in report" in {
      val dependency = randomDependency(withVersion = "2")

      report.report(DiffResult(Set(),Set(aRootDependencyNode(dependency)),Set(aRootDependencyNode(dependency)))) mustEqual UserAddedDepsConflictReport()
    }
  }

  val report = new ConflictReportCreator
}
