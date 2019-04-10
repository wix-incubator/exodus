package com.wixpress.build.maven

import org.eclipse.aether.graph.{DependencyVisitor, DependencyNode => AetherDependencyNode}
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator

class RetainNonConflictingDependencyNodesVisitor(visitor: PreorderNodeListGenerator) extends DependencyVisitor {
  private def wasRetainedInConflictResolution(node: AetherDependencyNode) =
    !node.getData.containsKey(ConflictResolver.NODE_DATA_WINNER)

  override def visitEnter(node: AetherDependencyNode): Boolean =
    wasRetainedInConflictResolution(node) && visitor.visitEnter(node)

  override def visitLeave(node: AetherDependencyNode): Boolean =
    visitor.visitLeave(node)
}