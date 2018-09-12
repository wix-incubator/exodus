package com.wix.bazel.migrator.model

sealed trait Target {
  def name: String

  //Is this actually absolute path?
  def belongingPackageRelativePath: String
}

sealed trait AnalyzedFromMavenTarget extends Target

object Target {

  case class TargetDependency(target: Target, isCompileDependency: Boolean)

  case class Jvm(name: String,
                 sources: Set[String],
                 belongingPackageRelativePath: String,
                 dependencies: Set[TargetDependency],
                 codePurpose: CodePurpose,
                 originatingSourceModule: SourceModule) extends Target

  case class ModuleDeps(
                         name: String,
                         belongingPackageRelativePath: String,
                         deps: Set[String],
                         runtimeDeps: Set[String],
                         testOnly: Boolean,
                         data: Set[String] = Set.empty,
                         exports: Set[String] = Set.empty
                       ) extends Target


  case class Resources(name: String,
                       belongingPackageRelativePath: String,
                       codePurpose: CodePurpose,
                       dependencies: Set[Target]) extends AnalyzedFromMavenTarget

  object Resources {
    private val AllCharactersButSlash = "[^/]*"
    private val ApplicabilityPattern = s"src/$AllCharactersButSlash/resources".r

    def applicablePackage(packageRelativePath: String): Boolean =
      ApplicabilityPattern.findFirstIn(packageRelativePath).isDefined

    def apply(name: String,
              belongingPackageRelativePath: String,
              dependencies: Set[Target] = Set.empty[Target]): Resources =
      Resources(
        name,
        belongingPackageRelativePath,
        CodePurpose(belongingPackageRelativePath, Seq(TestType.None)),
        dependencies
      )
  }

  case class Proto(name: String,
                   belongingPackageRelativePath: String,
                   dependencies: Set[Target],
                   originatingSourceModule: SourceModule) extends Target

  case class External(name: String,
                      belongingPackageRelativePath: String,
                      externalWorkspace: String) extends Target

  object External {
    private val externalTargetPattern = "@([^/]+)//([^:]+):(.+)".r("workspace", "relative-path", "target-name")

    def deserialize(label: String): Target.External =
      externalTargetPattern.findFirstMatchIn(label)
        .map(m =>
          Target.External(m.group("target-name"), belongingPackageRelativePath = m.group("relative-path"), m.group("workspace"))
        ).getOrElse(
        throw new RuntimeException(s"Could not deserialize String:$label")
      )
  }

}