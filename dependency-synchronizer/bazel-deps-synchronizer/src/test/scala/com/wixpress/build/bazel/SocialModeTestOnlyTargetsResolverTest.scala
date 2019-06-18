package com.wixpress.build.bazel

import java.nio.file.{Files, NoSuchFileException, Path}

import better.files.File
import com.wixpress.build.maven.Coordinates
import com.wixpress.hoopoe.test._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class SocialModeTestOnlyTargetsResolverTest extends SpecificationWithJUnit {

  "isTestOnlyTarget" should {

    "return true only in case given groupId and artifactId exists in list" in new Context {
      val artifactA = Coordinates(s"com.wixpress.aaa.$randomStr", "A-direct", "1.0.0")
      val artifactB = Coordinates(s"com.wixpress.bbb.$randomStr", "B-direct", "2.0.0")

      givenTestOnlyTargetsContain(artifactA)

      val testOnlyTargetsResolver: TestOnlyTargetsResolver =
        new SocialModeTestOnlyTargetsResolver(Some(managedDepsRepoPath.toString))

      testOnlyTargetsResolver.isTestOnlyTarget(artifactA) must beTrue
      testOnlyTargetsResolver.isTestOnlyTarget(artifactB) must beFalse
    }

    "throw NoSuchFileException in case test only targets file is missing from managed dep repo" in new Context {
      new SocialModeTestOnlyTargetsResolver(
        maybeManagedDepsRepoPath = Some(managedDepsRepoPath.toString)) must throwA[NoSuchFileException]
    }

    "not fail in case no managed deps repo path was supplied" in new Context {
      new SocialModeTestOnlyTargetsResolver(
        maybeManagedDepsRepoPath = None) must not(throwAn[Exception])
    }

    "not fail in case managed deps repo path is empty (expected from fakes...)" in new Context {
      new SocialModeTestOnlyTargetsResolver(
        maybeManagedDepsRepoPath = Some("")) must not(throwAn[Exception])
    }
  }

  trait Context extends Scope {
    val managedDepsRepoPath = Files.createTempDirectory("managedDepsRepoPath")

    private def givenTestOnlyTargetsFileExists(managedDepsRepoPath: Path) = {
      val managedDepsRepoSocialModeDirPath = Files.createDirectory(managedDepsRepoPath.resolve("social_mode"))
      Files.createFile(managedDepsRepoSocialModeDirPath.resolve("testonly_targets"))
    }

    def givenTestOnlyTargetsContain(artifact: Coordinates) = {
      val testOnlyTargetsFilePath = givenTestOnlyTargetsFileExists(managedDepsRepoPath)
      File(testOnlyTargetsFilePath).appendLine(artifact.serialized)
    }
  }

}
