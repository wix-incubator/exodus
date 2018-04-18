package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.wix.bazel.migrator.model.{Package, Target}

class SourcesPackageWriter(repoRoot: Path, bazelPackages: Set[Package]) {
  def write(): Unit = {
    bazelPackages
      .flatMap(jvmTargetsAndRelativePathFromMonoRepoRoot)
      .flatMap(sourceDirAndRelativePackagePaths)
      .map(sourcesTargetAndSourceDirPath)
      .foreach(writeSourcesTarget)
  }

  private def jvmTargetsAndRelativePathFromMonoRepoRoot(bazelPackage: Package): Set[JvmTargetAndRelativePath] = {
    val r = bazelPackage.targets.collect {
      case jvm: Target.Jvm => (jvm, bazelPackage.relativePathFromMonoRepoRoot)
    }

    r.map(JvmTargetAndRelativePathFromMonoRepoRoot(_))
  }

  def sourceDirAndRelativePackagePaths(jvmTargetAndRelativePath: JvmTargetAndRelativePath): Set[SourceDirPathAndRelativePackagePath] = {
    val basePackagePath = repoRoot.resolve(jvmTargetAndRelativePath.relativePath)
    jvmTargetAndRelativePath.jvm.sources.map { source =>
      val sourceDirPath = basePackagePath.resolve(adjustSource(source))
      SourceDirPathAndRelativePackagePath(sourceDirPath, jvmTargetAndRelativePath.relativePath)
    }
  }

  def sourcesTargetAndSourceDirPath(sourceDirPathAndRelativePackagePath: SourceDirPathAndRelativePackagePath): SourcesTargetAndSourceDirPath = {
      val sourcesTarget =
        s"""
           |filegroup(
           |    name = "sources",
           |    srcs = glob(["*.java"]) + glob(["*.scala"]),
           |    visibility = ["//${sourceDirPathAndRelativePackagePath.relativePackagePath}:__pkg__"]
           |)
           |
            """.
          stripMargin.getBytes
      val sourceDirBuildPath =
        sourceDirPathAndRelativePackagePath.sourceDirPath.resolve("BUILD.bazel")
      SourcesTargetAndSourceDirPath(sourceDirBuildPath, sourcesTarget)
    }

  private def writeSourcesTarget(a: SourcesTargetAndSourceDirPath) = {
    Files.write(
      a.sourceDirBuildPath,
      a.sourcesTarget,
      StandardOpenOption.CREATE, StandardOpenOption.APPEND
    )
  }

  private def adjustSource(source: String) = {
    if (source.startsWith("/"))
      source.drop(1)
    else
      source
  }

  private[migrator] case class SourceDirPathAndRelativePackagePath(sourceDirPath: Path, relativePackagePath: String)
  private[migrator] case class SourcesTargetAndSourceDirPath(sourceDirBuildPath: Path, sourcesTarget: Array[Byte])
  private[migrator] case class JvmTargetAndRelativePath(jvm: Target.Jvm, relativePath: String)
  private[migrator] object JvmTargetAndRelativePathFromMonoRepoRoot {
    def apply(targetAndRelativePath: (Target.Jvm, String)) =
      JvmTargetAndRelativePath(targetAndRelativePath._1, targetAndRelativePath._2)
  }
}
