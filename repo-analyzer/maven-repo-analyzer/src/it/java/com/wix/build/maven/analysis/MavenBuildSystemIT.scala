package com.wix.build.maven.analysis

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, SimpleFileVisitor}

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.model._
import com.wix.build.maven.analysis.MavenModule._
import com.wix.build.maven.analysis.SynchronizerConversion.MavenModule2ArtifactDescriptor
import com.wixpress.build.maven._
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.apache.maven.model.{Model, Parent, Dependency => MavenDependency}
import org.specs2.execute.{AsResult, Failure, Result}
import org.specs2.matcher.{AlwaysMatcher, Matcher}
import org.specs2.mutable.{Around, SpecificationWithJUnit}
import org.specs2.specification.{Scope => SpecsScope}

import scala.collection.JavaConverters._

//noinspection TypeAnnotation
class MavenBuildSystemIT extends SpecificationWithJUnit {

  "MavenBuildSystem modules should return" >> {
    trait ctx extends SpecsScope with Around {

      def registerInMavenRepository(modules: Iterable[MavenModule]): Unit =
        modules.foreach(addWithChildrenToArtifactory)

      private def addWithChildrenToArtifactory(module: MavenModule) = {
        fakeMavenRepository.addArtifacts(module.asArtifactDescriptor)
        registerInMavenRepository(module.modules.values)
      }

      def coordinatesOf(module: MavenModule): Coordinates =
        Coordinates(module.groupId.get, module.artifactId, module.version.get)


      override def around[R: AsResult](r: => R): Result = {
        repo.write()
        registerInMavenRepository(repo.rootModules)
        val result = AsResult(r)
        val changedMessageResult = addRepoContentsForFailure(result)
        fakeMavenRepository.stop()
        repo.close()
        changedMessageResult
      }

      private def addRepoContentsForFailure[R: AsResult](result: Result) =
        result match {
          case f: Failure => f.mapMessage(msg => s"\nrepo contents:\n${repo.contents}\n$msg")
          case _ => result
        }

      val fakeMavenRepository = new FakeMavenRepository(0)
      fakeMavenRepository.start()

      val resolver = new AetherMavenDependencyResolver(List(fakeMavenRepository.url), ignoreMissingDependenciesFlag = true)
      val buildSystem = new MavenBuildSystem(repo.root, dependencyResolver = resolver)

      def repo: Repo
    }

    "SourceModule in the repo for a single artifact repository" in new ctx {
      lazy val repo = Repo(MavenModule(groupId = "group-id",
        artifactId = "artifact-id",
        version = "some-version"))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(sourceModule(relativePathFromMonoRepoRoot = "",
        coordinates = be_===(Coordinates("group-id", "artifact-id", "some-version")))))
    }

    "SourceModules for the non pom modules in a multi module repository" in new ctx {
      lazy val repo = Repo(MavenModule(gavPrefix = "parent").withModules(
        modules = Map("child1" -> MavenModule(gavPrefix = "child1"), "child2" -> MavenModule(gavPrefix = "child2"))))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "child1", coordinates = moduleGavStartsWith("child1")),
        sourceModule(relativePathFromMonoRepoRoot = "child2", coordinates = moduleGavStartsWith("child2"))
      ))
    }

    "SourceModules for the pom modules that are not module aggregators" in new ctx {
      def pomCoordinates = Coordinates("group", "child1", "version", packaging = Packaging("pom"))
      lazy val repo = Repo(MavenModule(gavPrefix = "parent").withModules(
        modules = Map("child1" -> MavenModule(pomCoordinates))))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "child1", coordinates = equalTo(pomCoordinates))
      ))
    }

    "SourceModule for the non pom module recursively in a multi module repository" in new ctx {
      lazy val repo = Repo(
        MavenModule(gavPrefix = "grandparent").withModules(modules =
          Map("parent" -> MavenModule(gavPrefix = "parent").withModules(modules =
            Map("child" -> MavenModule(gavPrefix = "child")))
          )))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "parent/child", coordinates = moduleGavStartsWith("child"))))
    }

    "SourceModule in the repo for an artifact which gets its version from its parent" in new ctx {
      lazy val repo = Repo(MavenModule(
        groupId = Some("group"),
        artifactId = "artifact",
        version = None,
        parent = Some(MavenModule(gavPrefix = "parent"))))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(sourceModule(
        externalModule = be_===(Coordinates("group", "artifact", "parent-version")))))
    }

    "SourceModule in the repo for an artifact which gets its groupId from its parent" in new ctx {
      lazy val repo = Repo(MavenModule(
        groupId = None,
        artifactId = "artifact",
        version = Some("version"),
        parent = Some(MavenModule(gavPrefix = "parent"))))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(sourceModule(
        externalModule = be_===(Coordinates("parent-group", "artifact", "version")))))
    }

    "resource folders " in new ctx {
      def resourcesFolders = Set("src/main/resources", "src/test/resources", "src/it/resources", "src/e2e/resources")

      lazy val repo = Repo(SomeAggregatorModule.withModules(
        Map("rel" -> SomeCodeModule.withResourceFolders(resourcesFolders))))
      val sourceModules = buildSystem.modules()
      sourceModules must contain(sourceModule(resourcesPaths = equalTo(resourcesFolders)))
    }

    "only existing resource folders" in new ctx {
      def existingResourcesFolder = "src/it/resources"

      lazy val repo = Repo(
        SomeAggregatorModule.withModules(Map("rel" -> SomeCodeModule.withResourceFolders(Set(existingResourcesFolder)))))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(sourceModule(resourcesPaths = contain(exactly(existingResourcesFolder))))
    }

    "direct dependencies to artifacts that are not part of the repository" in new ctx {
      def someCoordinates = Coordinates("com.google.guava", "guava", "19")

      def someDependency = Dependency(someCoordinates, MavenScope.Compile)

      def someCoordinatesWithClassifier = Coordinates("com.example", "foo", "15", classifier = Some("classifier"))

      def someDependencyWithClassifier = Dependency(someCoordinatesWithClassifier, MavenScope.Compile)

      def someTestCoordinates = Coordinates("com.google.foo", "bar", "17")

      def someDependencyWithTestCoordinates = Dependency(someTestCoordinates, MavenScope.Test)

      lazy val repo = Repo(SomeCodeModule
        .withDependencyOn(someDependency, someDependencyWithClassifier)
        .withDependencyOn(someDependencyWithTestCoordinates))


      fakeMavenRepository.addArtifacts(ArtifactDescriptor.rootFor(someCoordinates),
        ArtifactDescriptor.rootFor(someCoordinatesWithClassifier),
        ArtifactDescriptor.rootFor(someTestCoordinates))
      val sourceModules = buildSystem.modules()

      sourceModules must contain(
        sourceModule(
          dependencies = moduleDependencies(
            directDependencies = contain(
              someDependency,
              someDependencyWithClassifier,
              someDependencyWithTestCoordinates))))
    }

    "direct dependencies to modules in repository" in new ctx {
      def someInternalModule = MavenMakers.someCoordinates("internal-module")

      def someDependency = Dependency(someInternalModule, MavenScope.Compile)

      def leafModule = MavenModule(someInternalModule)

      def interestingModule = MavenModule("interesting").withDependencyOn(someDependency)

      lazy val repo = Repo.withoutAggregatorAtRoot(
        "interesting" -> interestingModule,
        "internal-module" -> leafModule
      )


      val sourceModules = buildSystem.modules()

      sourceModules must contain(
        sourceModule(
          dependencies = moduleDependencies(
            directDependencies = contain(
              someDependency
            ))))
    }

    // TODO: all dependencies test

    "direct external dependencies with pom packaging" in new ctx {

      import com.wixpress.build.maven.Dependency

      def someCoordinates = Coordinates("some.package", "foo", "dont-care", packaging = Packaging("pom"))

      def someDependency = Dependency(someCoordinates, MavenScope.Compile)

      lazy val repo = Repo(SomeCodeModule.withDependencyOn(someDependency))
      fakeMavenRepository.addArtifacts(ArtifactDescriptor.rootFor(someCoordinates))

      val sourceModules = buildSystem.modules()

      sourceModules must contain(
        sourceModule(
          dependencies = moduleDependencies(
            directDependencies = contain(someDependency))))
    }

    "SourceModules even if no aggregator in root of repo by reading from the first level folders" in new ctx {
      lazy val repo = Repo.withoutAggregatorAtRoot(
        "sibling1" -> MavenModule(gavPrefix = "sibling1-parent").withModules(
          modules = Map("child1" -> MavenModule(gavPrefix = "sibling1-child1"), "child2" -> MavenModule(gavPrefix = "sibling1-child2"))),
        "sibling2" -> MavenModule(gavPrefix = "sibling2")
      )

      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "sibling1/child1", coordinates = moduleGavStartsWith("sibling1-child1")),
        sourceModule(relativePathFromMonoRepoRoot = "sibling1/child2", coordinates = moduleGavStartsWith("sibling1-child2")),
        sourceModule(relativePathFromMonoRepoRoot = "sibling2", coordinates = moduleGavStartsWith("sibling2"))
      ))
    }

    "SourceModules ignoring siblings without poms if no aggregator in root of repo" in new ctx {
      lazy val repo = Repo.withoutAggregatorAtRoot(
        "sibling1" -> MavenModule(gavPrefix = "sibling1")
      )

      Files.createDirectory(repo.root.resolve("sibling-without-pom-which-should-be-filtered"))

      val sourceModules = buildSystem.modules()
      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "sibling1", coordinates = moduleGavStartsWith("sibling1"))
      ))
    }

    "SourceModules with exclusion in dependencies" in new ctx {
      def someDependency = Dependency(someCoordinates, MavenScope.Compile, false, Set(Exclusion("excluded.group", "excluded-artifact")))

      lazy val repo = Repo(SomeCodeModule.withDependencyOn(someDependency))

      fakeMavenRepository.addArtifacts(ArtifactDescriptor.rootFor(someCoordinates))
      val sourceModules = buildSystem.modules()

      sourceModules must contain(
        sourceModule(
          dependencies = moduleDependencies(
            directDependencies = contain(someDependency))))
    }


    def someCoordinates = Coordinates("com.google.guava", "guava", "19")


    "a filtered SourceModules collection according to modulesToMute relative paths" in new ctx {
      lazy val repo = Repo(MavenModule(gavPrefix = "parent").withModules(
        modules = Map("child1" -> MavenModule(gavPrefix = "child1"), "child2_to_mute" -> MavenModule(gavPrefix = "child2"))))

      override val buildSystem = new MavenBuildSystem(repo.root, sourceModulesOverrides = SourceModulesOverrides("child2_to_mute"), resolver)
      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "child1", coordinates = moduleGavStartsWith("child1"))
      ))
    }

    "a filtered SourceModules collection of an entire sub-tree relative path" in new ctx {
      lazy val repo = Repo.withoutAggregatorAtRoot(
        "sibling1_to_mute" -> MavenModule(gavPrefix = "sibling1-parent").withModules(
          modules = Map("child1" -> MavenModule(gavPrefix = "sibling1-child1"), "child2" -> MavenModule(gavPrefix = "sibling1-child2"))),
        "sibling2" -> MavenModule(gavPrefix = "sibling2")
      )

      override val buildSystem = new MavenBuildSystem(repo.root, sourceModulesOverrides = SourceModulesOverrides("sibling1_to_mute"), resolver)
      val sourceModules = buildSystem.modules()

      sourceModules must contain(exactly(
        sourceModule(relativePathFromMonoRepoRoot = "sibling2", coordinates = moduleGavStartsWith("sibling2"))
      ))
    }
  }

  def sourceModule(relativePathFromMonoRepoRoot: String,
                   coordinates: Matcher[Coordinates]): Matcher[SourceModule] =
    sourceModule(be_===(relativePathFromMonoRepoRoot), coordinates)

  def sourceModule(
                    relativePathFromMonoRepoRoot: Matcher[String] = AlwaysMatcher[String](),
                    externalModule: Matcher[Coordinates] = AlwaysMatcher[Coordinates](),
                    resourcesPaths: Matcher[Set[String]] = AlwaysMatcher[Set[String]](),
                    dependencies: Matcher[ModuleDependencies] = AlwaysMatcher[ModuleDependencies]()
                  ): Matcher[SourceModule] =
    relativePathFromMonoRepoRoot ^^ {
      (_: SourceModule).relativePathFromMonoRepoRoot aka "relative path from mono repo root"
    } and
      externalModule ^^ {
        (_: SourceModule).coordinates aka "coordinates"
      } and
      resourcesPaths ^^ {
        (_: SourceModule).resourcesPaths aka "relative existing resources directories"
      } and
      dependencies ^^ {
        (_: SourceModule).dependencies aka "module dependencies"
      }

  def moduleDependencies(
                          directDependencies: Matcher[Set[Dependency]] = AlwaysMatcher[Set[Dependency]](),
                          allDependencies: Matcher[Set[Dependency]] = AlwaysMatcher[Set[Dependency]]()
                        ) = {
    directDependencies ^^ {
      (_: ModuleDependencies).directDependencies aka "direct external dependencies"
    } and
      allDependencies ^^ {
        (_: ModuleDependencies).allDependencies aka "all dependencies"
      }
  }

  def moduleGavStartsWith(prefix: String): Matcher[Coordinates] =
    startWith(prefix) ^^ {
      (_: Coordinates).groupId aka "groupId"
    } and
      startWith(prefix) ^^ {
        (_: Coordinates).artifactId aka "artifactId"
      } and
      startWith(prefix) ^^ {
        (_: Coordinates).version aka "version"
      }

}

object Repo {
  def apply(rootModule: MavenModule): Repo = new Repo(rootAggregatorModule = Some(rootModule))

  def withoutAggregatorAtRoot(siblingModules: (String, MavenModule)*): Repo = new Repo(siblingModules = siblingModules.toList)
}

case class Repo(rootAggregatorModule: Option[MavenModule] = None, siblingModules: List[(String, MavenModule)] = Nil) {
  private lazy val fileSystem = MemoryFileSystemBuilder.newLinux().build()
  private lazy val repoRoot = fileSystem.getPath("repoRoot")

  def root: Path = repoRoot

  def write(): Unit = rootAggregatorModule match {
    case Some(rootModule) => writeModule(repoRoot, rootModule)
    case None => siblingModules.foreach { case (relativePath, module) =>
      writeModule(repoRoot.resolve(relativePath), module)
    }
  }

  def rootModules: List[MavenModule] = rootAggregatorModule match {
    case Some(rootModule) => List(rootModule)
    case None => siblingModules.map(_._2)
  }

  def close(): Unit = fileSystem.close()

  def contents: String = {
    val accumulatedContents = new StringBuilder
    val visitor = new SimpleFileVisitor[Path] {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        accumulateName(file)
        super.visitFile(file, attrs)
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        accumulateName(dir)
        super.postVisitDirectory(dir, exc)
      }

      private def accumulateName(path: Path) = {
        accumulatedContents ++= path.toString
        accumulatedContents += '\n'
      }
    }
    fileSystem.getRootDirectories.asScala.foreach(Files.walkFileTree(_, visitor))
    accumulatedContents.result()
  }

  private def writeModule(modulePath: Path, mavenModule: MavenModule): Unit = {
    writeCurrentModule(modulePath, mavenModule)
    writeChildren(modulePath, mavenModule)
  }

  private def writeResourceDirectories(modulePath: Path, mavenModule: MavenModule): Unit = {
    mavenModule.resourceFolders.map(modulePath.resolve).foreach(Files.createDirectories(_))
  }

  private def writeCurrentModule(modulePath: Path, mavenModule: MavenModule): Unit = {
    Files.createDirectories(modulePath)
    writePom(modulePath, mavenModule)
    writeResourceDirectories(modulePath, mavenModule)
  }

  private def writePom(modulePath: Path, mavenModule: MavenModule): Unit = {
    val pomPath = modulePath.resolve("pom.xml")
    val writer = Files.newBufferedWriter(pomPath)
    val pom = pomFor(mavenModule)
    new MavenXpp3Writer().write(writer, pom)
  }

  private def writeChildren(modulePath: Path, mavenModule: MavenModule): Unit = {
    mavenModule.modules.foreach { case (relative, childModule) =>
      val childModulePath = modulePath.resolve(relative)
      writeModule(childModulePath, childModule)
    }
  }

  private def pomFor(module: MavenModule) = {
    val model = new Model()
    module.groupId.foreach(model.setGroupId)
    model.setArtifactId(module.artifactId)
    module.version.foreach(model.setVersion)
    module.parent.map(moduleToParent).foreach(model.setParent)
    module.packaging.foreach(p => model.setPackaging(p))
    module.dependencies.foreach { d =>
      val dep = new MavenDependency
      dep.setArtifactId(d.coordinates.artifactId)
      dep.setGroupId(d.coordinates.groupId)
      dep.setVersion(d.coordinates.version)
      d.coordinates.classifier.foreach(dep.setClassifier)
      Option(d.coordinates.packaging.value).filterNot(_ == "jar").foreach(dep.setType)
      dep.setScope(d.scope.name)
      model.addDependency(dep)
    }
    if (module.modules.nonEmpty) {
      model.setPackaging("pom")
      model.setModules(module.modules.keys.toList.asJava)
    }
    model
  }

  private def moduleToParent(mavenModule: MavenModule) = {
    val parent = new Parent
    mavenModule.groupId.foreach(parent.setGroupId)
    parent.setArtifactId(mavenModule.artifactId)
    mavenModule.version.foreach(parent.setVersion)
    parent
  }

}

case class MavenModule(groupId: Option[String] = None,
                       artifactId: String,
                       version: Option[String] = None,
                       packaging: Option[String] = None,
                       modules: Map[String, MavenModule] = Map.empty,
                       parent: Option[MavenModule] = None,
                       resourceFolders: Set[String] = Set.empty,
                       dependencies: Set[Dependency] = Set.empty) {
  def withDependencyOn(dependencies: Dependency*): MavenModule =
    copy(dependencies = this.dependencies ++ dependencies.toSet)

  //due to current limitation with how we work with Aether/FakeMavenRepository
  def resolvedGroupId: String = MavenModule.resolveGroupId(this).getOrElse(throw new IllegalStateException("no groupId defined"))

  def resolvedVersion: String = MavenModule.resolveVersion(this).getOrElse(throw new IllegalStateException("no version defined"))

  def withResourceFolders(resourceFolders: Set[String]): MavenModule = copy(resourceFolders = resourceFolders)

  def withModules(modules: Map[String, MavenModule]): MavenModule = copy(modules = modules)

  def withPackaging(packaging: String) = copy(packaging = Option(packaging))
}

object MavenModule {
  def apply(gavPrefix: String): MavenModule =
    MavenModule(Some(s"$gavPrefix-group"), s"$gavPrefix-artifact", Some(s"$gavPrefix-version"))

  def apply(groupId: String, artifactId: String, version: String): MavenModule =
    MavenModule(Option(groupId), artifactId, Option(version))

  def apply(coordinates: Coordinates): MavenModule = {
    println(coordinates)
    MavenModule(Some(coordinates.groupId), coordinates.artifactId, Some(coordinates.version), Some(coordinates.packaging.value))
  }

  val SomeCodeModule = MavenModule(gavPrefix = "some")
  val SomeAggregatorModule = MavenModule(gavPrefix = "aggregator")

  private def resolveGroupId(module: MavenModule): Option[String] = resolveProperty(module, m => m.groupId)

  private def resolveVersion(module: MavenModule): Option[String] = resolveProperty(module, m => m.version)

  @scala.annotation.tailrec
  private def resolveProperty(module: MavenModule, property: (MavenModule) => Option[String]): Option[String] = (property(module), module.parent) match {
    case (Some(version), _) => Some(version)
    case (None, None) => None
    case (None, Some(parent)) => resolveProperty(parent, property)
  }
}

object SynchronizerConversion {

  implicit class MavenModule2ArtifactDescriptor(module: MavenModule) {

    def toDependency(scopedCoordinates: (Scope, Set[Coordinates])): Set[Dependency] = {
      scopedCoordinates._2.map(toDependency(scopedCoordinates._1))
    }

    def toDependency(scope: Scope)(externalModule: Coordinates): Dependency = {
      val coordinates = Coordinates(
        groupId = externalModule.groupId,
        artifactId = externalModule.artifactId,
        version = externalModule.version,
        classifier = externalModule.classifier,
        packaging = externalModule.packaging)
      Dependency(coordinates, MavenScope.of(ScopeTranslation.toMaven(scope)))
    }

    def asArtifactDescriptor: ArtifactDescriptor = {
      val coordinates = Coordinates(groupId = module.resolvedGroupId, artifactId = module.artifactId, version = module.resolvedVersion)
      ArtifactDescriptor.anArtifact(coordinates, deps = module.dependencies.toList)
    }
  }

}
