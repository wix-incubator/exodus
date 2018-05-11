package com.wix.bazel.migrator.transform

import com.codota.service.model.DependencyInfo
import com.wix.bazel.migrator.model.SourceModule

class AnalyzeException(val failure: AnalyzeFailure) extends RuntimeException

object AnalyzeException {
  def unapply(analyzeException: AnalyzeException): Option[AnalyzeFailure] = Some(analyzeException.failure)
}

sealed trait AnalyzeFailure

object AnalyzeFailure {

  case class Composite(nested: List[AnalyzeFailure]) extends AnalyzeFailure

  case class SourceMissing(filePath: String, module: SourceModule) extends AnalyzeFailure

  case class ExternalBazelLabelMissing(coordinates: String, filepath: String) extends AnalyzeFailure

  case class MultipleSourceModules(modules: Iterable[SourceModule]) extends AnalyzeFailure

  case class MultipleMatchingDependencies(dependencies: Iterable[String]) extends AnalyzeFailure

  case class SomeException(err: Throwable) extends AnalyzeFailure

  case class MissingAnalysisInCodota() extends AnalyzeFailure

  case class AugmentedFailure(failure: AnalyzeFailure, metadata: FailureMetadata) extends AnalyzeFailure

  implicit class AugmentedAnalyzeFailureEither[A](either: Either[AnalyzeFailure, A]) {
    def augment(metadata: => FailureMetadata): Either[AnalyzeFailure, A] = either.left.map(f => AnalyzeFailure.AugmentedFailure(f, metadata))
  }

}


sealed trait FailureMetadata

object FailureMetadata {

  case class SourceModuleFilePath(filePath: String, module: SourceModule) extends FailureMetadata

  case class InternalDep(internalDepGroup: Iterable[DependencyInfo.OptionalInternalDependency]) extends FailureMetadata

  case class InternalDepMissingExtended(depInfo: List[DependencyInfo]) extends FailureMetadata

  case class MissingArtifactInCodota(artifactName: String) extends FailureMetadata
}
