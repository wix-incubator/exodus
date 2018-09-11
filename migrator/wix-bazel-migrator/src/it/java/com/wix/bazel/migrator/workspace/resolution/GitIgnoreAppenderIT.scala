package com.wix.bazel.migrator.workspace.resolution

import java.nio.file.{Files, Paths}

import org.specs2.matcher.{Matcher, TraversableMatchers}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

// should move to WorkspaceResolution+Writers E2E
class GitIgnoreAppenderIT extends SpecificationWithJUnit {
  "GitIgnoreAppender" should {

    "add content to pre-existing .gitignore" in new ctx {
      localWorkspaceRepo.initWithGitIgnore(initialGitIgnoreContent)

      appendToGitIgnore(contentToBeIgnored)

      mimicWorkspaceResolutionByWriting(contentToBeIgnored)

      localWorkspaceRepo.stageAndCommitAll

      verifyIsNotTracked(contentToBeIgnored)
    }

    "create .gitignore file with new content when file does not exist" in new ctx {
      appendToGitIgnore(contentToBeIgnored)

      mimicWorkspaceResolutionByWriting(contentToBeIgnored)

      localWorkspaceRepo.stageAndCommitAll

      verifyIsNotTracked(contentToBeIgnored)
    }

    "do nothing when .gitignore already has requested content" in new ctx {
      localWorkspaceRepo.initWithGitIgnore(contentToBeIgnored)

      appendToGitIgnore(contentToBeIgnored)

      val updatedGitIgnoreContent = new String(Files.readAllBytes(localRepoDir.resolve(".gitignore")))

      updatedGitIgnoreContent mustEqual contentToBeIgnored
    }
  }

  trait ctx extends Scope {
    def verifyIsNotTracked(untrackedFile: String) = {
      localWorkspaceRepo must haveCommitsWith(".gitignore")
      localWorkspaceRepo must not(haveCommitsWith(untrackedFile))
    }

    def appendToGitIgnore(content: String) = {
      val appender = new GitIgnoreAppender(localRepoDir)
      appender.append(content)
    }

    def mimicWorkspaceResolutionByWriting(fileName: String) = {
      Files.createDirectory(localRepoDir.resolve("tools"))
      Files.write(localRepoDir.resolve(fileName), "commits_content".getBytes("UTF-8"))
    }

    val localWorkspaceRepo = FakeLocalRepository.newBlankRepository
    val localRepoDir = Paths.get(localWorkspaceRepo.localRepoDir)

    val initialGitIgnoreContent = "/bazel-*"

    val contentToBeIgnored = "tools/commits.bzl"

    def haveCommitsWith(file: String): Matcher[FakeLocalRepository] = {
      TraversableMatchers.contain(file) ^^ {(_:FakeLocalRepository).allRepoCommits().flatMap(_.changedFiles)}
    }
  }
}
