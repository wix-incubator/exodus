package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.analyze.Code
import com.wix.bazel.migrator.transform.GraphSupport._

private[transform] case class GraphAndCodes(graph: CodeGraph, keyToCodes: Map[Vertex, Set[Code]])
