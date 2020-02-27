package com.wix.bazel.migrator.utils

import com.wix.bazel.migrator.transform.failures.AnalyzeFailure

import scala.util.{Failure, Success}

object Retry {

  def eitherRetry[A, E <: Throwable](n: Int = 20)(fn: => A): Either[AnalyzeFailure, A] = {
    tryRetry[A, E](n)(fn) match {
      case Success(s) => Right(s)
      case Failure(e) => Left(AnalyzeFailure.SomeException(e))
    }
  }

  @annotation.tailrec
  def tryRetry[A, E <: Throwable](n: Int = 20, butDoNotRetryIf: E => Boolean = alwaysRetry)(fn: => A): util.Try[A] = {
    util.Try {
      fn
    } match {
      case x: util.Success[A] => x
      case util.Failure(y:E) if butDoNotRetryIf(y) => Failure(y)
      case _ if n > 1 => tryRetry[A,E](n - 1)(fn)
      case failure => failure
    }
  }
  private val alwaysRetry: Throwable => Boolean = (_) => false
}
