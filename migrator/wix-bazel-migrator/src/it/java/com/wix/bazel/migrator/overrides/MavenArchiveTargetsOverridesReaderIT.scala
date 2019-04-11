package com.wix.bazel.migrator.overrides

import java.nio.file.Path

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wixpress.build.bazel.OverrideCoordinates
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope


class MavenArchiveTargetsOverridesReaderIT extends SpecificationWithJUnit {
  "MavenArchiveTargetsOverridesReader" should {

    "return empty set in case override file does not exists" in {
      lazy val fileSystem = MemoryFileSystemBuilder.newLinux().build()
      val repoRoot: Path = fileSystem.getPath("repoRoot")
      MavenArchiveTargetsOverridesReader.from(repoRoot) mustEqual MavenArchiveTargetsOverrides(Set.empty)
    }

    "throw exception in case of invalid json" in new ctx {
      val overridesPath = setupOverridesPath(repoRoot, "maven_archive_targets.overrides")
      writeOverrides("blabla")

      MavenArchiveTargetsOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "return empty set in case of empty array in the json" in new ctx {
      val overridesPath = setupOverridesPath(repoRoot, "maven_archive_targets.overrides")
      val json = s"""|{
                     |  "unpackedOverridesToArchive" : []
                     |}""".stripMargin
      writeOverrides(json)
      MavenArchiveTargetsOverridesReader.from(repoRoot) mustEqual MavenArchiveTargetsOverrides(Set.empty)
    }

    "return set of maven archive coordinates to override" in new ctx {
      val overridesPath = setupOverridesPath(repoRoot, "maven_archive_targets.overrides")
      val json = s"""{
                     |  "unpackedOverridesToArchive": [
                     |    {
                     |      "groupId": "some-group",
                     |      "artifactId": "some-artifact-id"
                     |    },
                     |    {
                     |      "groupId": "another-group",
                     |      "artifactId": "another-artifact-id"
                     |    }
                     |  ]
                     |}""".stripMargin

      writeOverrides(json)
      MavenArchiveTargetsOverridesReader.from(repoRoot) mustEqual MavenArchiveTargetsOverrides(Set(
        OverrideCoordinates("some-group", "some-artifact-id"),
        OverrideCoordinates("another-group", "another-artifact-id")))
    }
  }
  trait ctx extends Scope with OverridesReaderITSupport
}
