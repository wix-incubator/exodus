package com.wixpress.build.bazel

import better.files.File
import com.wixpress.build.sync.e2e.{Commit, FakeRemoteRepository}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class GitBazelRepositoryIT extends SpecificationWithJUnit {
  "GitBazelRepository" should {

    "reset whatever was in given path to clone of given git URL" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val someLocalPath = aRandomTempDirectory
      someLocalPath.createChild("some-file.txt").overwrite("some content")

      new GitBazelRepository(fakeRemoteRepository.remoteURI, someLocalPath)

      someLocalPath.list must contain(exactly(someLocalPath / "WORKSPACE", someLocalPath / ".git"))
    }

    "create local path (including parents) even if they did not exit beforehand" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val nonExistingLocalPath = aRandomTempDirectory / "plus" / "some" / "new" / "subdirectories"

      new GitBazelRepository(fakeRemoteRepository.remoteURI, nonExistingLocalPath)

      eventually {
        (nonExistingLocalPath / "WORKSPACE").exists aka "WORKSPACE was checked out" must beTrue
      }
    }

    "return valid bazel local workspace" in {
      val workspaceFileContent = "some workspace file content"
      val fakeRemoteRepository = aFakeRemoteRepoWithWorkspaceFile(workspaceFileContent)
      val gitBazelRepository = new GitBazelRepository(fakeRemoteRepository.remoteURI, aRandomTempDirectory)

      val localWorkspace = gitBazelRepository.localWorkspace("master")

      localWorkspace.workspaceContent() mustEqual workspaceFileContent
    }

    "persist file change to remote git repository" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val someLocalPath = File.newTemporaryDirectory("clone")
      val gitBazelRepository = new GitBazelRepository(fakeRemoteRepository.remoteURI, someLocalPath)

      val fileName = "some-file.txt"
      val content = "some content"
      someLocalPath.createChild(fileName).overwrite(content)

      val branchName = "some-branch"
      gitBazelRepository.persist(branchName, Set(fileName), "some message")

      fakeRemoteRepository.updatedContentOfFileIn(branchName, fileName) must beSuccessfulTry(content)
    }

    "throw exception when persising to base branch (master)" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val someLocalPath = File.newTemporaryDirectory("clone")
      val gitBazelRepository = new GitBazelRepository(fakeRemoteRepository.remoteURI, someLocalPath)

      val fileName = "some-file.txt"
      someLocalPath.createChild(fileName).overwrite("some content")

      val baseBranch = "master"
      gitBazelRepository.persist("master", Set(fileName), "some message") must throwA[RuntimeException]
    }

    "overwrite any file in target branch with the persist content" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val someLocalPath = File.newTemporaryDirectory("clone")
      val gitBazelRepository = new GitBazelRepository(fakeRemoteRepository.remoteURI, someLocalPath)

      val branchName = "some-branch"

      gitBazelRepository.localWorkspace("master").overwriteWorkspace("old-content")
      gitBazelRepository.persist(branchName, Set("WORKSPACE"), "some message")

      val newContent = "new-content"
      gitBazelRepository.localWorkspace("master").overwriteWorkspace(newContent)
      gitBazelRepository.persist(branchName, Set("WORKSPACE"), "some message")

      fakeRemoteRepository.updatedContentOfFileIn(branchName, "WORKSPACE") must beSuccessfulTry(newContent)
    }

    "persist with commit message with given username and email that will be visible on remote repository" in new fakeRemoteRepositoryWithEmptyWorkspace {
      val someLocalPath = File.newTemporaryDirectory("clone")
      val username = "someuser"
      val email = "some@email.com"
      val gitBazelRepository = new GitBazelRepository(fakeRemoteRepository.remoteURI, someLocalPath, username, email)
      val fileName = "some-file.txt"
      someLocalPath.createChild(fileName).overwrite("some content")
      val someMessage = "some message"

      private val branchName = "some-branch"
      gitBazelRepository.persist(branchName, Set(fileName), someMessage)
      val expectedCommit = Commit(
        username = username,
        email = email,
        message = someMessage,
        changedFiles = Set(fileName)
      )

      fakeRemoteRepository.allCommitsForBranch(branchName) must contain(expectedCommit)
    }
  }

  trait fakeRemoteRepositoryWithEmptyWorkspace extends Scope {
    val fakeRemoteRepository = new FakeRemoteRepository
    fakeRemoteRepository.initWithWorkspaceFileContent("")
  }

  private def aRandomTempDirectory = {
    val dir = File.newTemporaryDirectory("local-clone")
    dir.toJava.deleteOnExit()
    dir
  }

  private def aFakeRemoteRepoWithWorkspaceFile(workspaceFileContent: String) =
    (new FakeRemoteRepository).initWithWorkspaceFileContent(workspaceFileContent)


}
