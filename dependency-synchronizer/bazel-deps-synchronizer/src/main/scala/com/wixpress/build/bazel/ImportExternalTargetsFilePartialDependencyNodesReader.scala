package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.EitherSupport.partitionEithers
import com.wixpress.build.bazel.ImportExternalTargetsFileReader._
import com.wixpress.build.maven.{Coordinates, Dependency, DependencyNode, MavenScope}

case class ImportExternalTargetsFilePartialDependencyNodesReader(content: String, localWorkspaceName: String = "") {
  def allBazelDependencyNodes(): Set[PartialDependencyNode] = {
    splitToStringsWithJarImportsInside(content).flatMap(parse).toSet
  }

  private def parse(importExternalTarget: String) = {
    parseCoordinates(importExternalTarget).map(validatedCoordinates => {
      val exclusions = extractExclusions(importExternalTarget)
      val compileDeps = extractListByAttribute(CompileTimeDepsFilter, importExternalTarget)
      val runtimeDeps = extractListByAttribute(RunTimeDepsFilter, importExternalTarget)

      PartialDependencyNode(Dependency(validatedCoordinates.coordinates, MavenScope.Compile, exclusions),
        compileDeps.flatMap(d => parseTargetDependency(d, MavenScope.Compile)) ++
          runtimeDeps.flatMap(d => parseTargetDependency(d, MavenScope.Runtime))
      )
    })
  }

  def parseTargetDependency(dep: String, scope: MavenScope): Option[PartialDependency] = {
    dep match {
      case PomAggregateDependencyLabel(ruleName) => Some(PartialPomAggregateDependency(ruleName, scope))
      case LocalSourceDependencyLabel() => None
      case _ => Some(PartialJarDependency(parseImportExternalDep(dep).getOrElse(dep), scope))
    }
  }

  object PomAggregateDependencyLabel {
    def unapply(label: String): Option[String] = parseAggregatePomDep(label).map(_.replace("/", "_").replace(":", "_"))

    private def parseAggregatePomDep(text: String) = {
      AggregatePomDepFilter.findFirstMatchIn(text).map(_.group("ruleName"))
    }

    private val AggregatePomDepFilter = """@.*//third_party/(.*)""".r("ruleName")
  }

  object LocalSourceDependencyLabel {
    def unapply(label: String): Boolean = {
      val maybeMatch = FullyQualifiedLocalSourceDepFilter(localWorkspaceName).findFirstMatchIn(label)
      val stillMaybeMatch = maybeMatch.fold(LocalSourceDepFilter.findFirstMatchIn(label))(m => Option(m))
      stillMaybeMatch.isDefined
    }

    private def FullyQualifiedLocalSourceDepFilter(localWorkspaceName: String) = ("""@""" + localWorkspaceName + """//(.*)""").r
    private def LocalSourceDepFilter = """@//(.*)""".r
  }
}

case class AllImportExternalFilesDependencyNodesReader(filesContent: Set[String],
                                                       pomAggregatesCoordinates: Set[Coordinates],
                                                       externalDeps: Set[Dependency] = Set(),
                                                       localWorkspaceName: String = "") {
  def allMavenDependencyNodes(): Either[Set[DependencyNode],Set[String]] = {
    val bazelDependencyNodes = filesContent.flatMap(c => ImportExternalTargetsFilePartialDependencyNodesReader(c, localWorkspaceName).allBazelDependencyNodes())
    val baseDependencies = bazelDependencyNodes.map(_.baseDependency)
    val dependencyNodesOrErrors = bazelDependencyNodes.map(d => mavenDependencyNodeFrom(d, baseDependencies))

    transformEithers(dependencyNodesOrErrors)
  }

  private def mavenDependencyNodeFrom(partialNode: PartialDependencyNode, baseDependencies: Set[Dependency]):Either[DependencyNode,Set[String]] = {
    val dependenciesOrErrors = partialNode.targetDependencies.map(t => transitiveDepFrom(t, baseDependencies, partialNode.baseDependency.coordinates))

    val (maybeDependencies, rights) = partitionEithers[Option[Dependency],String](dependenciesOrErrors)
    if (rights.nonEmpty)
      Right(rights)
    else
      Left(DependencyNode(
        partialNode.baseDependency,
        maybeDependencies.flatten))
  }

  private def transitiveDepFrom(partialDep: PartialDependency, baseDependencies: Set[Dependency], dependantArtifact: Coordinates):Either[Option[Dependency], String] = {
    def lookupDep: Option[Dependency] = {
      partialDep match {
        case _: PartialPomAggregateDependency => pomAggregatesCoordinates.find(_.workspaceRuleName == partialDep.ruleName).map(Dependency(_, partialDep.scope))
        case _ => val maybeDependency = baseDependencies.find(_.coordinates.workspaceRuleName == partialDep.ruleName)
          maybeDependency.fold(externalDeps.find(_.coordinates.workspaceRuleName == partialDep.ruleName))(x => Option(x))
      }
    }

    val maybeDependency = lookupDep.map(_.copy(scope = partialDep.scope))
    if (maybeDependency.isEmpty)
      Right(s"missing artifact information for: $partialDep.\nThe dependant artifact is ${dependantArtifact.serialized}")
    else
      Left(maybeDependency)
  }

  private def transformEithers(dependencyNodesOrErrors: Set[Either[DependencyNode, Set[String]]]) = {
    val (lefts, rights) = partitionEithers(dependencyNodesOrErrors)
    if (rights.nonEmpty)
      Right(rights.flatten)
    else
      Left(lefts)
  }
}

case class PartialDependencyNode(baseDependency: Dependency, targetDependencies: Set[PartialDependency])

trait PartialDependency {
  val ruleName: String
  val scope: MavenScope
}

case class PartialJarDependency(ruleName: String, scope: MavenScope) extends PartialDependency

case class PartialPomAggregateDependency(ruleName: String, scope: MavenScope) extends PartialDependency

object EitherSupport {
  def partitionEithers[A,B](eithers: Set[Either[A, B]]): (Set[A], Set[B]) = {
    def lefts = eithers collect { case Left(x) => x }
    val rights = eithers collect { case Right(x) => x }
    (lefts, rights)
  }
}