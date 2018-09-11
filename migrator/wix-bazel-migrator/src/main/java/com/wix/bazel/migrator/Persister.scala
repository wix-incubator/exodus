package com.wix.bazel.migrator

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.time.Instant
import java.time.temporal.TemporalUnit
import java.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.{CodePurpose, Package, Target, TestType}
import com.wix.bazel.migrator.utils.{IgnoringIsArchiveDefMixin, IgnoringIsProtoArtifactDefMixin, TypeAddingMixin}
import com.wix.build.maven.analysis.SourceModules
import com.wixpress.build.maven.{Coordinates, MavenScope, Packaging}

import scala.collection.JavaConverters._

object Persister {

  private val transformedFile = new File("dag.bazel")
  private val mavenCache = Paths.get("classpathModules.cache")
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
    .addMixIn(classOf[Target], classOf[TypeAddingMixin])
    .addMixIn(classOf[CodePurpose], classOf[TypeAddingMixin])
    .addMixIn(classOf[TestType], classOf[TypeAddingMixin])
    .addMixIn(classOf[MavenScope], classOf[TypeAddingMixin])
    .addMixIn(classOf[Packaging], classOf[IgnoringIsArchiveDefMixin])
    .addMixIn(classOf[Coordinates], classOf[IgnoringIsProtoArtifactDefMixin])

  def persistTransformationResults(bazelPackages: Set[Package]): Unit = {
    println("Persisting transformation")
    objectMapper.writeValue(transformedFile, bazelPackages)
  }

  def readTransformationResults(): Set[Package] = {
    val collectionType = objectMapper.getTypeFactory.constructCollectionType(classOf[util.Collection[Package]], classOf[Package])
    val value: util.Collection[Package] = objectMapper.readValue(transformedFile, collectionType)
    val bazelPackages = value.asScala.toSet
    bazelPackages
  }

  def persistMavenClasspathResolution(sourceModules: SourceModules): Unit = {
    println("Persisting maven")
    objectMapper.writeValue(mavenCache.toFile, sourceModules)
  }

  def readTransMavenClasspathResolution(): SourceModules = {
    objectMapper.readValue[SourceModules](mavenCache.toFile, classOf[SourceModules])
  }

  def mavenClasspathResolutionIsUnavailableOrOlderThan(amount: Int, unit: TemporalUnit): Boolean =
    !Files.isReadable(mavenCache) ||
      lastModifiedMavenCache().toInstant.isBefore(Instant.now().minus(amount, unit))

  private def lastModifiedMavenCache() =
    Files.readAttributes(mavenCache, classOf[BasicFileAttributes]).lastModifiedTime()

}