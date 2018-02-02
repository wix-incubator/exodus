package com.wixpress.build.bazel

import java.io.FileNotFoundException

import better.files._
import com.wixpress.build.bazel.ThirdPartyOverridesMakers.runtimeOverrides
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class FileSystemBazelLocalWorkspaceIT extends SpecificationWithJUnit {
  "FileSystemBazelLocalWorkspace" should {
    "throw exception when given filepath does not exist" in {
      val nonExistingPath = file"/not-very-likely-to-exists-path"

      new FileSystemBazelLocalWorkspace(nonExistingPath) must throwA[FileNotFoundException]
    }

    "return empty workspace content if WORKSPACE does not exists" in new blankWorkspaceCtx {
      new FileSystemBazelLocalWorkspace(blankWorkspaceRootPath).workspaceContent() mustEqual ""
    }

    "Get workspace file content" in new blankWorkspaceCtx {
      val workspaceContent = "some content"
      blankWorkspaceRootPath.createChild("WORKSPACE").overwrite(workspaceContent)

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).workspaceContent() mustEqual workspaceContent
    }

    "Get BUILD.bazel file content given package that exist on path" in new blankWorkspaceCtx {
      blankWorkspaceRootPath.createChild("WORKSPACE")
      val packageName = "some/package"
      val buildFile = blankWorkspaceRootPath / packageName / "BUILD.bazel"
      buildFile.createIfNotExists(createParents = true)
      val buildFileContent = "some build content"
      buildFile.overwrite(buildFileContent)

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).buildFileContent(packageName) must beSome(buildFileContent)
    }

    "return None if BUILD.bazel file does not exists" in new blankWorkspaceCtx {
      blankWorkspaceRootPath.createChild("WORKSPACE")
      val packageName = "some/non-existing/package"

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).buildFileContent(packageName) must beNone
    }

    "return empty third party overrides if no such file exists" in new blankWorkspaceCtx {
      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).thirdPartyOverrides() mustEqual ThirdPartyOverrides.empty
    }

    "return serialized third party overrides according to json in local workspace" in new blankWorkspaceCtx {
      val originalOverrides = runtimeOverrides(OverrideCoordinates("some.group","some-artifact"),"label")
      val json = {
        val objectMapper = ThirdPartyOverridesReader.mapper
        objectMapper.writeValueAsString(originalOverrides)
      }
      (blankWorkspaceRootPath / "bazel_migration" / "third_party_targets.overrides")
        .createIfNotExists(createParents = true)
        .overwrite(json)

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).thirdPartyOverrides() mustEqual originalOverrides
    }

    "write WORKSPACE file content" in new blankWorkspaceCtx {
      val workspaceFile = blankWorkspaceRootPath.createChild("WORKSPACE")
      val newContent = "newContent"

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).overwriteWorkspace(newContent)

      workspaceFile.contentAsString mustEqual newContent
    }

    "write BUILD.bazel file content, even if the package did not exist" in new blankWorkspaceCtx {
      val workspaceFile = blankWorkspaceRootPath.createChild("WORKSPACE")
      val newPackage = "some/new/package"
      val buildFileContent = "some build file content"

      aFileSystemBazelLocalWorkspace(blankWorkspaceRootPath).overwriteBuildFile(newPackage, buildFileContent)
      val buildFile = blankWorkspaceRootPath / newPackage / "BUILD.bazel"

      buildFile.exists aka "build file exists" must beTrue
      buildFile.contentAsString mustEqual buildFileContent
    }
  }


  trait blankWorkspaceCtx extends Scope {
    val blankWorkspaceRootPath = File.newTemporaryDirectory("bazel")
    blankWorkspaceRootPath
      .toJava.deleteOnExit()
  }

  private def aFileSystemBazelLocalWorkspace(on: File) = {
    new FileSystemBazelLocalWorkspace(on)
  }
}
