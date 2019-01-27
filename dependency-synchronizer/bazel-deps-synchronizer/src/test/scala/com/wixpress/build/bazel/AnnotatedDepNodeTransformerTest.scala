package com.wixpress.build.bazel

import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.{Coordinates, DependencyNode}
import org.specs2.matcher.{AlwaysMatcher, Matcher}
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class AnnotatedDepNodeTransformerTest extends SpecificationWithJUnit {

  "AnnotatedDependencyNodeTransformer" should {
    "return import external rule with 'linkable' transitive runtime dep if came from global overrided list and is missing from local neverlink " in {
      val transformer = new AnnotatedDependencyNodeTransformer(new NeverLinkResolver(transitiveDeps, localNeverlinkDependencies = Set()))

      transformer.annotate(runtimeDepNode) must beAnnotatedDependencyNode(
        anArtifact = be_===(artifact),
        runtimeDeps = contain(ImportExternalDep(transitiveDep, linkableSuffixNeeded = true)),
        compileDeps = beEmpty)
    }

    "return import external rule with 'linkable' transitive compile dep if came from global overrided list and is missing from local neverlink " in {
      val transformer = new AnnotatedDependencyNodeTransformer(new NeverLinkResolver(transitiveDeps, localNeverlinkDependencies = Set()))

      transformer.annotate(compileTimeDepNode) must beAnnotatedDependencyNode(
        anArtifact = be_===(artifact),
        runtimeDeps = beEmpty,
        compileDeps = contain(ImportExternalDep(transitiveDep, linkableSuffixNeeded = true))
      )
    }

    "return import external rule with transitive runtime dep without 'linkable' suffix if it can be found both on global and local neverlink lists" in {
      val transformer = new AnnotatedDependencyNodeTransformer(new NeverLinkResolver(transitiveDeps, localNeverlinkDependencies = transitiveDeps))

      transformer.annotate(runtimeDepNode) must beAnnotatedDependencyNode(
        anArtifact = be_===(artifact),
        runtimeDeps = contain(ImportExternalDep(transitiveDep, linkableSuffixNeeded = false)),
        compileDeps = beEmpty
      )
    }

    "return import external rule with transitive compiletime dep without 'linkable' suffix if it can be found both on global and local neverlink lists" in {
      val transformer = new AnnotatedDependencyNodeTransformer(new NeverLinkResolver(transitiveDeps, localNeverlinkDependencies = transitiveDeps))

      transformer.annotate(compileTimeDepNode) must beAnnotatedDependencyNode(
        anArtifact = be_===(artifact),
        runtimeDeps = beEmpty,
        compileDeps = contain(ImportExternalDep(transitiveDep, linkableSuffixNeeded = false))
      )
    }
  }

  val artifact = someCoordinates("some-artifact")
  val transitiveDep = someCoordinates("some-transitiveDep")
  val transitiveDeps = Set(transitiveDep)
  val runtimeDepNode = DependencyNode(asCompileDependency(artifact),Set(asRuntimeDependency(transitiveDep)))
  val compileTimeDepNode = DependencyNode(asCompileDependency(artifact),Set(asCompileDependency(transitiveDep)))



  def beAnnotatedDependencyNode(anArtifact: Matcher[Coordinates] = AlwaysMatcher[Coordinates](),
                           runtimeDeps: Matcher[Set[BazelDep]] = AlwaysMatcher[Set[BazelDep]](),
                           compileDeps: Matcher[Set[BazelDep]] = AlwaysMatcher[Set[BazelDep]]()
             ): Matcher[AnnotatedDependencyNode] =
    anArtifact ^^ {
      (_: AnnotatedDependencyNode).baseDependency.coordinates aka "artifact"
    } and runtimeDeps ^^ {
      (_: AnnotatedDependencyNode).runtimeDependencies aka "runtimeDeps"
    } and compileDeps ^^ {
      (_: AnnotatedDependencyNode).compileTimeDependencies aka "compileTimeDeps"
    }
}
