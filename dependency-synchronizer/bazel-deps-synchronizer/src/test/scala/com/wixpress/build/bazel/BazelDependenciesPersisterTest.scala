package com.wixpress.build.bazel

import com.wixpress.build.maven.MavenMakers
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class BazelDependenciesPersisterTest extends SpecificationWithJUnit {
  "BazelDependenciesPersister should persist files with appropriate message" >> {
    trait ctx extends Scope {
      val branch = "master"
      val header = "some header"
      val bazelRepository = new FakeBazelRepository()
      val persister = new BazelDependenciesPersister(header, bazelRepository)
    }

    "given a single dependency and a single file path" in new ctx {
      val changedFiles = Set("some-file")
      val coordinates = MavenMakers.someCoordinates("artifact")
      val dependenciesSet = Set(coordinates)

      persister.persistWithMessage(changedFiles, dependenciesSet)

      bazelRepository.lastCommit should beEqualTo(DummyCommit(
        branchName = branch,
        changedFilePaths = changedFiles,
        message =
          s"""$header
             | - ${coordinates.serialized}
             |""".stripMargin))
    }

    "given multiple files and dependencies" in new ctx {
      val changedFiles = Set("file1", "file2")
      val someDependencies = {
        1 to 5
      }.map(index => MavenMakers.someCoordinates(s"artifact-$index")).reverse.toSet

      persister.persistWithMessage(changedFiles, someDependencies)

      bazelRepository.lastCommit should beEqualTo(DummyCommit(
        branchName = branch,
        changedFilePaths = changedFiles,
        message =
          s"""$header
             |${someDependencies.map(_.serialized).toSeq.sorted.map(c => s" - $c").mkString("\n")}
             |""".stripMargin))
    }
  }
}

class FakeBazelRepository() extends BazelRepository {
  private val commits = collection.mutable.ListBuffer.empty[DummyCommit]

  def lastCommit: DummyCommit = commits.last

  override def localWorkspace(): BazelLocalWorkspace = {
    throw new RuntimeException("this class is only for dummy commits purpose")
  }

  override def persist(branchName: String, changedFilePaths: Set[String], message: String): Unit = {
    commits.append(DummyCommit(branchName, changedFilePaths, message))
  }

}

case class DummyCommit(branchName: String, changedFilePaths: Set[String], message: String)
