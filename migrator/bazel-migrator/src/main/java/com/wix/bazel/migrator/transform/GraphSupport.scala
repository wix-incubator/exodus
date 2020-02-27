package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.analyze.Code
import org.jgrapht
import org.jgrapht.graph.DefaultDirectedGraph

private[transform] trait GraphSupport {

  type Vertex = ResourceKey

  case class Edge(source: Vertex, target: Vertex, isCompileDependency: Boolean)

  type CodeGraph = jgrapht.DirectedGraph[Vertex, Edge]

  object CodeGraph {
    def empty: CodeGraph = new DefaultDirectedGraph(EdgeFactory)
  }

  type AcyclicCodeGraph = jgrapht.experimental.dag.DirectedAcyclicGraph[Vertex, Edge]

  object AcyclicCodeGraph {
    def empty: AcyclicCodeGraph = new AcyclicCodeGraph(EdgeFactory)
  }

  object EdgeFactory extends jgrapht.EdgeFactory[Vertex, Edge] {
    override def createEdge(v1: Vertex, v2: Vertex): Edge =
      throw new UnsupportedOperationException("Edges shouldn't be automatically created, supply one yourself")
  }

  type CodesMap = Map[ResourceKey, Set[Code]]

  type ResourceKeyMap = Map[ResourceKey, ResourceKey]
}

private[transform] object GraphSupport extends GraphSupport
