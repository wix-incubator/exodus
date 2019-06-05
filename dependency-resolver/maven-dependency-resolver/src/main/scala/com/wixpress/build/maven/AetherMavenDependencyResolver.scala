package com.wixpress.build.maven

import java.nio.file.{Files, Path}

import better.files.File
import com.wixpress.build.maven.AetherDependencyConversions._
import com.wixpress.build.maven.resolver.ManualRepositorySystemFactory
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.artifact.{Artifact, DefaultArtifact}
import org.eclipse.aether.collection.{CollectRequest, CollectResult, DependencyCollectionException}
import org.eclipse.aether.graph.{Dependency => AetherDependency, DependencyNode => AetherDependencyNode, Exclusion => AetherExclusion}
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository.{Builder => RemoteRepositoryBuilder}
import org.eclipse.aether.resolution.{ArtifactDescriptorException, ArtifactDescriptorRequest, ArtifactDescriptorResult}
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

class AetherMavenDependencyResolver(remoteRepoURLs: => List[String],
                                    localRepoPath: File = AetherMavenDependencyResolver.tempLocalRepoPath()
                                   ) extends MavenDependencyResolver {

  private val repositorySystem = ManualRepositorySystemFactory.newRepositorySystem

  private val artifactDescriptorStore = mutable.Map[Artifact, ArtifactDescriptorResult]() withDefault artifactDescriptorOf

  override def managedDependenciesOf(artifact: Coordinates): List[Dependency] = {
    val artifactDescriptor = artifactDescriptorStore(artifact.asAetherArtifact)
    orderedUniqueDependenciesFrom(artifactDescriptor.getManagedDependencies.asScala)
  }

  private def orderedUniqueDependenciesFrom(dependencies: Iterable[AetherDependency]): List[Dependency] = {
    val deps = dependencies
      .map(_.asDependency)
      .map(validatedDependency)

    val orderedDepSet = new mutable.LinkedHashSet[Dependency]()
    for (dep <- deps){
      orderedDepSet.add(dep)
    }
    orderedDepSet.toList
  }

  override def directDependenciesOf(coordinates: Coordinates): List[Dependency] =
    directDependenciesOf(artifactFromCoordinates(coordinates))

  private def artifactFromCoordinates(artifact: Coordinates) =
    new DefaultArtifact(artifact.groupId, artifact.artifactId, artifact.packaging.value, artifact.version)

  def directDependenciesOf(pathToPom: Path): List[Dependency] =
    directDependenciesOf(artifactFromPath(pathToPom))

  private def directDependenciesOf(artifact: DefaultArtifact) = {
    val artifactDescriptor = artifactDescriptorStore(artifact)
    orderedUniqueDependenciesFrom(artifactDescriptor.getDependencies.asScala)
  }

  private def artifactDescriptorOf(artifact: Artifact): ArtifactDescriptorResult = {
    val request = descriptorRequest(artifact)
    val res = withSession(ignoreMissingDependencies = false, repositorySystem.readArtifactDescriptor(_, request))
    artifactDescriptorStore.update(artifact, res)

    res
  }

  private def descriptorRequest(of: Artifact): ArtifactDescriptorRequest = {
    val artifactReq = new ArtifactDescriptorRequest
    artifactReq.setArtifact(of)
    artifactReq.setRepositories(remoteRepositories)
    artifactReq
  }

  private def artifactFromPath(pathToPom: Path) = {
    val project = (new MavenXpp3Reader).read(Files.newBufferedReader(pathToPom))
    val version = (maybeVersionFromPom(project) orElse maybeVersionFromParent(project))
      .getOrElse(throw new RuntimeException("could not parse version from pom"))
    val groupId = (Option(project.getGroupId) orElse Option(project.getParent).map(_.getGroupId))
      .getOrElse(throw new RuntimeException("could not parse groupId from pom"))
    val artifact = new DefaultArtifact(groupId, project.getArtifactId, "", version)
    artifact
  }

  private def maybeVersionFromPom(project: Model) = {
    Option(project.getVersion)
  }

  private def maybeVersionFromParent(project: Model) = {
    Option(project.getParent).map(_.getVersion)
  }

  override def dependencyClosureOf(baseDependencies: List[Dependency], withManagedDependencies: List[Dependency], ignoreMissingDependencies: Boolean = true): Set[DependencyNode] = {
    try {
      withSession(ignoreMissingDependencies = ignoreMissingDependencies, session => {
        prioritizeManagedDeps(session)
        val aetherResponse = repositorySystem.collectDependencies(session, collectRequestOf(baseDependencies, withManagedDependencies))
        dependencyNodesOf(aetherResponse)
          .map(fromAetherDependencyNode)
          .toSet
      })
    }
    catch {
      case e: DependencyCollectionException =>
        throw new IllegalArgumentException(s"""|${e.getCause()}
                                               |===== Please double check that you have no typos.
                                               |if you REALLY meant to reference this jar and you know it exists even though there IS NO pom,
                                               |please rerun the tool with --ignoreMissingDependencies flag at the end =====
                                               |""".stripMargin)
    }
  }

  private def fromAetherDependencyNode(node: AetherDependencyNode): DependencyNode = {
    DependencyNode(
      baseDependency = node.getDependency.asDependency,
      dependencies = dependenciesSetFromDependencyNodes(node.getChildren.asScala).toSet
    )
  }

  private def dependenciesSetFromDependencyNodes(dependencyNodes: Iterable[AetherDependencyNode]): List[Dependency] = {
    orderedUniqueDependenciesFrom(dependencyNodes.map(_.getDependency))
  }

  private def withSession[T](ignoreMissingDependencies:Boolean, f: DefaultRepositorySystemSession => T): T = {
    val localRepo = new LocalRepository(localRepoPath.pathAsString)
    val session = MavenRepositorySystemUtils.newSession
    session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(ignoreMissingDependencies, false))
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo))
    val result = Try(f(session)) recover {
      case e: ArtifactDescriptorException => throw new MissingPomException(e.getMessage,e)
      case e => throw e
    }
    result.get
  }

  private def dependencyNodesOf(collectResult: CollectResult): mutable.Seq[AetherDependencyNode] = {
    val preOrder = new PreorderNodeListGenerator
    val visitor = new RetainNonConflictingDependencyNodesVisitor(preOrder)
    collectResult.getRoot.accept(visitor)
    val list = preOrder.getNodes.asScala
    list
  }

  private def prioritizeManagedDeps(on: DefaultRepositorySystemSession) = {
    on.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true)
    on.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true)
  }

  private def collectRequestOf(baseDependencies: List[Dependency], withManagedDependencies: List[Dependency]) = {
    val managedDeps = withManagedDependencies
      .map(_.asAetherDependency).asJava
    val dependencies = baseDependencies.map(_.asAetherDependency).toList.asJava
    (new CollectRequest)
      .setDependencies(dependencies)
      .setManagedDependencies(managedDeps)
      .setRepositories(remoteRepositories)
  }

  private def remoteRepositories = {
    def mapper(repo: (String, Int)) = {
      val repoURL = repo._1
      val repoIndex = repo._2
      new RemoteRepositoryBuilder(s"repo$repoIndex", "default", repoURL).build()
    }

    val repoList = remoteRepoURLs.zipWithIndex.map(mapper).asJava
    repoList
  }

}

object AetherMavenDependencyResolver {
  private def tempLocalRepoPath(): File = {
    val tempDir = File.newTemporaryDirectory("local-repo")
    tempDir.toJava.deleteOnExit()
    tempDir
  }
}

object TestAetherResolver extends App {
  // checks that only runtime/compile scope deps suffice to start that instance
  val aetherResolver = new AetherMavenDependencyResolver(List("https://repo1.maven.org/maven2"))
}

object AetherDependencyConversions {

  implicit class `AetherDependency --> Dependency`(aetherDependency: AetherDependency) {
    def asDependency: Dependency =
      Dependency(
        coordinates = aetherDependency.getArtifact.asCoordinates,
        scope = MavenScope.of(aetherDependency.getScope),
        isNeverLink = false,
        exclusions = aetherDependency.getExclusions.asScala.map(_.asExclusion).toSet
      )

  }

  implicit class `Dependency --> AetherDependency`(dep: Dependency) {
    def asAetherDependency: AetherDependency = new AetherDependency(
      dep.coordinates.asAetherArtifact,
      dep.scope.name,
      false,
      dep.exclusions.map(_.asAetherExclusion).asJava)

  }


  implicit class `Coordinates --> AetherArtifact`(c: Coordinates) {
    def asAetherArtifact: Artifact = new DefaultArtifact(c.serialized)
  }

  implicit class `AetherArtifact --> Coordinates`(artifact: Artifact) {
    def asCoordinates: Coordinates =
      Coordinates(
        artifact.getGroupId,
        artifact.getArtifactId,
        artifact.getVersion,
        Packaging(Option(artifact.getExtension).filter(!_.isEmpty).getOrElse("jar")),
        Option(artifact.getClassifier).filter(!_.isEmpty))
  }

  implicit class `Exclusion --> AetherExclusion`(exclusion: Exclusion) {
    private val AnyWildCard = "*"

    def asAetherExclusion: AetherExclusion =
      new AetherExclusion(exclusion.groupId, exclusion.artifactId, AnyWildCard, AnyWildCard)
  }


  implicit class `AetherExclusion --> Exclusion`(aetherExclusion: AetherExclusion) {
    def asExclusion: Exclusion = Exclusion(aetherExclusion.getGroupId, aetherExclusion.getArtifactId)
  }

}
