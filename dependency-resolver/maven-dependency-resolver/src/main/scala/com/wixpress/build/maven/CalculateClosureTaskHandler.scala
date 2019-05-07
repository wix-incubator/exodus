package com.wixpress.build.maven

import com.wixpress.build.maven.ApiConversions.toDependency
import com.wixpress.hoopoe.ids.Guid

class CalculateClosureTaskHandler(resolver: MavenDependencyResolver,
                                  completedTasksHandler: CompletedTaskHandler) {

  def handle(task: CalculateClosureTask): Unit = {
    val baseDeps: Set[Dependency] = task.request.baseDependencies.map(toDependency).toSet
    val managedDeps: Set[Dependency] = task.request.managedDependencies.map(toDependency).toSet
    val dependencyNodes: Set[DependencyNode] = resolver.dependencyClosureOf(baseDeps, managedDeps)
    completedTasksHandler.onComplete(task.jobId, dependencyNodes)
  }

}

trait CompletedTaskHandler {
  def onComplete(jobId: Guid[CalculateClosureTask], dependencyNodes: Set[DependencyNode]): Unit
}