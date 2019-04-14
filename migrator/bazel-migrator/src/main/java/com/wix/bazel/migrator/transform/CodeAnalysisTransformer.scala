package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Target.TargetDependency
import com.wix.bazel.migrator.model.{Package, SourceModule, Target}
import com.wix.bazel.migrator.transform.GraphSupport._
import org.jgrapht.alg.GabowStrongConnectivityInspector

import scala.collection.JavaConverters._
import scala.collection.mutable


class CodeAnalysisTransformer(dependencyAnalyzer: DependencyAnalyzer) {

  def transform(modules: Set[SourceModule]): Set[model.Package] = {
    val cyclicGraphAndCodes = buildDirectedGraph(modules)
    val dagAndCodes = cyclicToAcyclic(cyclicGraphAndCodes)
    val packages = dagToBazel(dagAndCodes)
    packages
  }

  private def buildDirectedGraph(modules: Set[SourceModule]): GraphAndCodes = {
    val keyToCodes = newCodesMap
    val graph = CodeGraph.empty
    modules.foreach(addModuleTo(graph, keyToCodes))
    keyToCodes.foreach{
      case (key, value) => println(s"========\n${key.codeDirPath.module.coordinates}+${key.resourcePackage}")
    }
    GraphAndCodes(graph, asImmutableMap(keyToCodes))
  }

  private def addModuleTo(graph: CodeGraph,
                          keyToCodes: mutable.MultiMap[Vertex, Code])(module: SourceModule): Unit = {
    dependencyAnalyzer.allCodeForModule(module).foreach { code =>
      val resourceKey = ResourceKey.fromCodePath(code.codePath)
      val coordinates = resourceKey.codeDirPath.module.coordinates
      val str = resourceKey.resourcePackage
      println(s"========\n${coordinates}+${str}")
      
      keyToCodes.addBinding(resourceKey, code)

      graph.addVertex(resourceKey)

      code.dependencies.foreach { dependency =>
        val dependencyResourceKey = ResourceKey.fromCodePath(dependency.codePath)
        graph.addVertex(dependencyResourceKey)
        val edge = Edge(resourceKey, dependencyResourceKey, dependency.isCompileDependency)
        graph.addEdge(resourceKey, dependencyResourceKey, edge)
      }
    }
  }

  private def dagToBazel(graphAndCodes: GraphAndCodes): Set[model.Package] = {
    val duplicatePackagesWithSameRelativePath = graphAndCodes.graph.vertexSet().asScala.toSet.map(toPackage(graphAndCodes.graph, graphAndCodes.keyToCodes))
    val packagesGroupedByRelativePath = duplicatePackagesWithSameRelativePath.groupBy(_.relativePathFromMonoRepoRoot)
    packagesGroupedByRelativePath.mapValues(collectTargets).values.toSet
  }

  //TODO The following are utility methods- need to think if they need to be here, in Target/Package or in enhance my class pattern
  //Maybe btw on ResourceKey since they work on that. Maybe ResourceKey.toPackageWithSingleTarget
  private def toPackage(graph: CodeGraph, keyToCodes: CodesMap)(resourceKey: ResourceKey): Package = {
    Package(resourceKey.packageRelativePath, Set(toTarget(graph, keyToCodes)(resourceKey)), resourceKey.codeDirPath.module)
  }

  private def collectTargets(packagesWithSameRelativePath: Set[Package]): Package =
    packagesWithSameRelativePath.reduce { (p1, p2) => p1.copy(targets = p1.targets ++ p2.targets) }

  private def toTarget(graph: CodeGraph, keyToCodes: CodesMap)(resourceKey: ResourceKey): Target = {
    val targets = targetDependencies(graph, keyToCodes, resourceKey)
    resourceKey.toTarget(keyToCodes, targets.toSet)
  }

  private def targetDependencies(graph: CodeGraph, keyToCodes: CodesMap, resourceKey: Vertex): mutable.Set[TargetDependency] = {
    val outgoingResourceKeys = graph.outgoingEdgesOf(resourceKey).asScala
    val targets = outgoingResourceKeys.map(toTargetDependency(graph, keyToCodes))
    targets
  }

  private def toTargetDependency(graph: CodeGraph, keyToCodes: CodesMap)(edge: Edge): TargetDependency =
    TargetDependency(edge.target.toTarget(keyToCodes, Set.empty), edge.isCompileDependency)

  private def cyclicToAcyclic(graphAndCodes: GraphAndCodes): GraphAndCodes = {
    val cycles = findCyclesIn(graphAndCodes.graph)
    val cyclicVertexToCompositeVertex = mapCyclicVerticesToCompositeVertices(cycles)
    val compositeVertexToCodes = mapCompositeVerticesToCodesFrom(cyclicVertexToCompositeVertex, graphAndCodes.keyToCodes)
    val dag = buildDAG(graphAndCodes.graph, cyclicVertexToCompositeVertex)
    GraphAndCodes(dag, compositeVertexToCodes)
  }

  private def mapCompositeVerticesToCodesFrom(cyclicVertexToCompositeVertex: ResourceKeyMap,
                                              cyclicVertexToCodes: CodesMap): CodesMap = {
    val cyclicKeysGroupedByCompositeKeys = cyclicVertexToCodes.groupBy { case (r, _) => cyclicVertexToCompositeVertex(r) }

    cyclicKeysGroupedByCompositeKeys.mapValues(collectCodes)
  }

  private def collectCodes(map: CodesMap): Set[Code] = map.values.flatten.toSet

  private def findCyclesIn(directedGraph: CodeGraph): Seq[CodeGraph] = {
    val strongConnectivityInspector = new GabowStrongConnectivityInspector(directedGraph)
    val connectedSubGraphs = strongConnectivityInspector.stronglyConnectedSubgraphs().asScala
    connectedSubGraphs
  }

  private def buildDAG(directedGraph: CodeGraph,
                       simpleVertexToCompositeVertex: ResourceKeyMap): AcyclicCodeGraph = {
    val dag = AcyclicCodeGraph.empty
    simpleVertexToCompositeVertex.values.foreach(dag.addVertex)
    directedGraph.edgeSet().asScala.foreach { edge =>
      val compositeSource = simpleVertexToCompositeVertex(edge.source)
      val compositeTarget = simpleVertexToCompositeVertex(edge.target)
      if (compositeSource != compositeTarget)
        dag.addDagEdge(compositeSource, compositeTarget, Edge(compositeSource, compositeTarget, edge.isCompileDependency))
    }
    dag
  }

  private def mapCyclicVerticesToCompositeVertices(connectedSubGraphs: Seq[CodeGraph]): ResourceKeyMap = {
    connectedSubGraphs.flatMap(subGraph => {
      val subGraphVertexSet = subGraph.vertexSet().asScala
      val combinedResourceKey = try {
        subGraphVertexSet.reduce(ResourceKey.combine)
      } catch {
        case e: IllegalArgumentException => throw new IllegalArgumentException(s"${e.getMessage}\n full subGraph containing cycle=${subGraphVertexSet.map(_.withoutExternalDeps)}\n")
      }
      subGraphVertexSet.map(_ -> combinedResourceKey)
    }).toMap
  }

  private def newCodesMap: mutable.MultiMap[Vertex, Code] = {
    new mutable.HashMap[Vertex, mutable.Set[Code]] with mutable.MultiMap[Vertex, Code]
  }

  private def asImmutableMap(mm: mutable.MultiMap[Vertex, Code]) = mm.mapValues(_.toSet).toMap
}
