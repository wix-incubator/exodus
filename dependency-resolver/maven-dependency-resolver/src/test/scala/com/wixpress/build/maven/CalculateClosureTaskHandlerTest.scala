package com.wixpress.build.maven

import com.wixpress.build.maven.dependency.resolver.api.v1.DependenciesClosureRequest
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.wixpress.hoopoe.ids._
import org.specs2.mock.Mockito
import ApiConversions._

class CalculateClosureTaskHandlerTest extends SpecificationWithJUnit with Mockito {

  "handle" should {

    "calculate dependency closure and call onComplete with the result" in new Context {
      val baseDeps = Set(Dependency(Coordinates("base.group.id", "base.artifact.id", "base.version"), MavenScope.Compile))
      val managedDeps = Set(
        Dependency(Coordinates("managed1.group.id", "managed1.artifact.id", "managed1.version"), MavenScope.Compile),
        Dependency(Coordinates("managed2.group.id", "managed2.artifact.id", "managed2.version"), MavenScope.Compile)
      )
      val task = aTaskFor(baseDeps, managedDeps)
      resolver.dependencyClosureOf(baseDeps, managedDeps) returns closure

      handler.handle(task)

      there was one(completedHandler).onComplete(task.jobId, closure)
    }
  }

  abstract class Context extends Scope {
    val resolver = mock[MavenDependencyResolver]
    val completedHandler = mock[CompletedTaskHandler]
    val closure: Set[DependencyNode] = Set(
      DependencyNode(
        Dependency(Coordinates("res1.group.id", "res1.artifact.id", "res1.version"), MavenScope.Compile),
        Set(Dependency(Coordinates("res2.group.id", "res2.artifact.id", "res2.version"), MavenScope.Compile))
      ))

    val handler = new CalculateClosureTaskHandler(resolver, completedHandler)

    def aTaskFor(baseDeps: Set[Dependency], managedDeps: Set[Dependency]): CalculateClosureTask = {
      val request = DependenciesClosureRequest(baseDeps.map(toMavenDependency).toSeq, managedDeps.map(toMavenDependency).toSeq)
      CalculateClosureTask(randomGuid[CalculateClosureTask], request)
    }
  }

}
