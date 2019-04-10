package com.wix.bazel.migrator.model

object ScopeTranslation {
  private val mavenBazelScopeMappings = Map("TEST" -> Scope.TEST_COMPILE,
    "COMPILE" -> Scope.PROD_COMPILE,
    "RUNTIME" -> Scope.PROD_RUNTIME,
    "PROVIDED" -> Scope.PROVIDED)

  private val bazelMavenScopeMappings = Map(Scope.TEST_COMPILE -> "TEST",
    Scope.PROD_COMPILE -> "COMPILE",
    Scope.PROD_RUNTIME -> "RUNTIME",
    Scope.PROVIDED -> "PROVIDED")

  def fromMaven(scope: String): Scope = mavenBazelScopeMappings(scope.toUpperCase)
  def toMaven(scope: Scope): String = bazelMavenScopeMappings(scope)
}
