package com.wix.bazel.migrator.transform

import java.nio.file.Path

import com.wix.bazel.migrator.model.Target.TargetDependency
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.transform.GraphSupport.CodesMap

//TODO rename & move SourceCodeDirPath to something like SourceCode.DirPath and have CodePath be SourceCode.Path or SourceCode.FilePath
private[transform] case class SourceCodeDirPath(module: SourceModule, relativeSourceDirPathFromModuleRoot: String)

private[transform] case class ResourceKey(codeDirPath: SourceCodeDirPath, resourcePackage: String, sharedSubPackages: Set[String] = Set.empty) {

  def withoutExternalDeps: ResourceKey = {
    val dependencies = codeDirPath.module.dependencies.copy(scopedDependencies = Map.empty)
    val newCodeDir = codeDirPath.copy(module = codeDirPath.module.copy(dependencies = dependencies))
    this.copy(codeDirPath = newCodeDir)
  }

  //The below is ugly but Paths.get(...) took twice as much (10 minutes compares to 5) on the fw migration
  def packageRelativePath: String =
      relativePathFromMonoRepoRootOf(codeDirPath) +
      codeDirPath.relativeSourceDirPathFromModuleRoot +
      resourcePackageSuffix

  private def resourcePackageSuffix = if (resourcePackage.isEmpty) "" else "/" + resourcePackage

  private def relativePathFromMonoRepoRootOf(codeDirPath: SourceCodeDirPath): String =
    if (codeDirPath.module.relativePathFromMonoRepoRoot.isEmpty)
      ""
    else
      codeDirPath.module.relativePathFromMonoRepoRoot + "/"

  def toTarget(keyToCodes: CodesMap, targetDependencies: Set[TargetDependency]): Target = {
    //TODO would really like to extract this somewhere. feels like a different level of abstraction but not sure where and more importantly how to name it
    val (name, sources) = if (sharedSubPackages.isEmpty)
      (lowestSourcePackage(resourcePackage), Set("."))
    else {
      val subPackagesWithDotsInsteadOfEmptyStrings = sharedSubPackages.map(replaceEmptyStringWithDot)
      (concatSubPackagesToCompositeName(subPackagesWithDotsInsteadOfEmptyStrings), subPackagesWithDotsInsteadOfEmptyStrings)
    }

    if (Target.Resources.applicablePackage(packageRelativePath))
      Target.Resources("resources", packageRelativePath, targetDependencies.map(_.target))
    else {
      val codes = keyToCodes.getOrElse(this, throw new IllegalStateException(s"can't find resourceKey = $this \n in keyToCodes map: $keyToCodes")).view
      val codePurpose = CodePurpose(packageRelativePath, codes.map(_.testType))
      Target.Jvm(name, sources, packageRelativePath, targetDependencies, codePurpose, codeDirPath.module)
    }
  }

  private def replaceEmptyStringWithDot(s: String) = if (s.isEmpty) "." else s

  private def concatSubPackagesToCompositeName(packages: Set[String]) = "agg=" + packages.map(_.stripPrefix("/")).toSeq.sorted.mkString("+")

  private def lowestSourcePackage(sourcePackage: String): String = sourcePackage.split('/').last

  private def canonicalSubPackages = if (sharedSubPackages.isEmpty) Set(resourcePackage) else sharedSubPackages.map(resourcePackage + _)
}

private[transform] object ResourceKey {

  def fromCodePath(sourceCodePath: CodePath): ResourceKey = {
    ResourceKey(SourceCodeDirPath(sourceCodePath.module, sourceCodePath.relativeSourceDirPathFromModuleRoot), extractSourcePackageFrom(sourceCodePath.filePath))
  }

  def combine(source: ResourceKey, target: ResourceKey): ResourceKey = {
    if (source.codeDirPath != target.codeDirPath) {
      throw new IllegalArgumentException(s"Trying to combine\n source=${source.withoutExternalDeps}\n target=${target.withoutExternalDeps}\n creates a cycle between two different modules or two top level source dirs and that isn't supported\n")
    }

    val canonicalSubPackages = source.canonicalSubPackages ++ target.canonicalSubPackages
    val sharedPackage = commonPrefix(canonicalSubPackages)
    val relativeSubPackages = canonicalSubPackages.map(_.stripPrefix(sharedPackage))
    ResourceKey(source.codeDirPath, sharedPackage, relativeSubPackages)
  }

  private def extractSourcePackageFrom(filePath: Path): String =
    Option(filePath.getParent).map(_.toString).getOrElse("")

  private def commonPrefix(packages: Set[String]): String = {
    //val noSlashPackages = packages.toSeq.map(_.replace('/', Char.MinValue))
    //TODO commonPrefix performance dilemma
    // min/max reduce twice over the sequence. need to benchmark and see if this is costly
    //  a few alternatives:
    //      Consider Using a sorted set? [i'm not sure last is implemented efficiently enough though] [also need to think about maybe giving it an ordering to handle the '/' when we tackle it
    //      Consider just reducing ourselves and returning a tuple of min max
    //      consider sorted and then using head and last (still with the same problem like above of last)
    //TODO what about handling a case where a package name is "less" than '/' in string ordering terms and we have another package which is more. This will ruin this min and max. one solution is to replace '/' with Char.MinValue and the beginning
    packages.min.split('/').zip(packages.max.split('/')).takeWhile {
      case (a, b) => a == b
    }.map {
      case (a, b) => a
    }.mkString("/")
  }
}